package com.urr.app.battle.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.urr.app.battle.BattleAppService;
import com.urr.commons.exception.BizException;
import com.urr.commons.exception.ErrorCode;
import com.urr.domain.action.ActionDefEntity;
import com.urr.domain.battle.*;
import com.urr.domain.battle.dto.*;
import com.urr.domain.item.ItemDefEntity;
import com.urr.domain.item.PlayerItemStackEntity;
import com.urr.domain.player.PlayerEntity;
import com.urr.domain.wallet.WalletEntity;
import com.urr.domain.wallet.WalletFlowEntity;
import com.urr.infra.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class BattleAppServiceImpl implements BattleAppService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final String ACTION_KIND_INSTANT = "INSTANT";
    private static final String REWARD_TYPE_CURRENCY = "currency";
    private static final String REWARD_TYPE_ITEM = "item";
    private static final String WALLET_REASON_DUNGEON = "DUNGEON";
    private static final String WALLET_REF_TYPE_DUNGEON = "DUNGEON";
    private static final int ITEM_LOCATION_BAG = 1;
    private static final int RUN_RESULT_SUCCESS = 1;

    private final PlayerMapper playerMapper;
    private final ActionDefMapper actionDefMapper;
    private final DungeonDefMapper dungeonDefMapper;
    private final EncounterDefMapper encounterDefMapper;
    private final EncounterWaveMapper encounterWaveMapper;
    private final ItemDefMapper itemDefMapper;
    private final WalletMapper walletMapper;
    private final WalletFlowMapper walletFlowMapper;
    private final PlayerItemStackMapper playerItemStackMapper;
    private final DungeonRunLogMapper dungeonRunLogMapper;
    private final PlayerDungeonProgressMapper playerDungeonProgressMapper;

    @Override
    @Transactional
    public BattleStartResultDTO startBattle(Long accountId, Long playerId, String actionCode) {
        if (accountId == null) {
            throw new IllegalArgumentException("未登录");
        }
        if (playerId == null) {
            throw new IllegalArgumentException("playerId不能为空");
        }
        if (!StringUtils.hasText(actionCode)) {
            throw new IllegalArgumentException("actionCode不能为空");
        }

        PlayerEntity player = requirePlayer(playerId);
        if (!accountId.equals(player.getAccountId())) {
            throw new BizException(ErrorCode.CONFLICT, "无权限操作该角色");
        }

        ActionDefEntity action = requireAction(actionCode.trim());
        validateAction(action);

        ActionParamsDTO actionParams = readJson(action.getParamsJson(), ActionParamsDTO.class);
        if (actionParams == null || !StringUtils.hasText(actionParams.getDungeonCode())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "动作未配置dungeonCode");
        }

        DungeonDefEntity dungeon = requireDungeon(actionParams.getDungeonCode());
        DungeonMetaDTO dungeonMeta = readJson(dungeon.getMetaJson(), DungeonMetaDTO.class);
        String encounterCode = StringUtils.hasText(actionParams.getEncounterCode())
                ? actionParams.getEncounterCode()
                : (dungeonMeta == null ? null : dungeonMeta.getEncounterCode());
        if (!StringUtils.hasText(encounterCode)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "副本未配置encounterCode");
        }

        EncounterDefEntity encounter = requireEncounter(encounterCode);
        List<EncounterWaveEntity> waves = requireWaves(encounter.getId());

        int requiredLevel = Math.max(nullSafe(action.getMinPlayerLevel()), nullSafe(dungeon.getMinLevel()));
        if (nullSafe(player.getLevel()) < requiredLevel) {
            throw new BizException(ErrorCode.CONFLICT, "玩家等级不足");
        }

        int energyCost = dungeon.getEnergyCost() != null ? dungeon.getEnergyCost() : nullSafe(action.getBaseEnergyCost());
        if (nullSafe(player.getEnergy()) < energyCost) {
            throw new BizException(ErrorCode.CONFLICT, "体力不足");
        }

        int beforeEnergy = nullSafe(player.getEnergy());
        deductEnergy(player, energyCost);
        int afterEnergy = nullSafe(player.getEnergy());

        Map<String, BattleRewardItemDTO> totalRewardMap = new LinkedHashMap<>();
        List<BattleWaveResultDTO> waveResults = new ArrayList<>();

        for (EncounterWaveEntity wave : waves) {
            List<BattleRewardItemDTO> waveRewards = settleWaveRewards(player, dungeon, wave, totalRewardMap, accountId);
            BattleWaveResultDTO waveResult = new BattleWaveResultDTO();
            waveResult.setWaveNo(wave.getWaveNo());
            waveResult.setRewards(waveRewards);
            waveResults.add(waveResult);
        }

        List<BattleRewardItemDTO> totalRewards = new ArrayList<>(totalRewardMap.values());
        insertRunLog(player, dungeon, waves.size(), energyCost, totalRewards, waveResults, accountId);
        upsertProgress(player, dungeon, waves.size(), accountId);

        BattleStartResultDTO result = new BattleStartResultDTO();
        result.setPlayerId(player.getId());
        result.setActionCode(action.getActionCode());
        result.setDungeonCode(dungeon.getDungeonCode());
        result.setEncounterCode(encounter.getEncounterCode());
        result.setResult("SUCCESS");
        result.setEnergyCost(energyCost);
        result.setBattleTimeMs(dungeon.getBattleTimeMs());
        result.setBeforeEnergy(beforeEnergy);
        result.setAfterEnergy(afterEnergy);
        result.setWaves(waveResults);
        result.setTotalRewards(totalRewards);
        return result;
    }

    private PlayerEntity requirePlayer(Long playerId) {
        PlayerEntity player = playerMapper.selectById(playerId);
        if (player == null || isDeleted(player.getDeleteFlag())) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return player;
    }

    private ActionDefEntity requireAction(String actionCode) {
        ActionDefEntity action = actionDefMapper.selectOne(new LambdaQueryWrapper<ActionDefEntity>()
                .eq(ActionDefEntity::getActionCode, actionCode)
                .last("limit 1"));
        if (action == null || isDeleted(action.getDeleteFlag())) {
            throw new BizException(ErrorCode.NOT_FOUND, "动作不存在");
        }
        return action;
    }

    private void validateAction(ActionDefEntity action) {
        if (action.getStatus() == null || action.getStatus() != 1) {
            throw new BizException(ErrorCode.CONFLICT, "动作未启用");
        }
        if (!ACTION_KIND_INSTANT.equalsIgnoreCase(action.getActionKind())) {
            throw new BizException(ErrorCode.CONFLICT, "当前动作不是即时战斗入口");
        }
    }

    private DungeonDefEntity requireDungeon(String dungeonCode) {
        DungeonDefEntity dungeon = dungeonDefMapper.selectOne(new LambdaQueryWrapper<DungeonDefEntity>()
                .eq(DungeonDefEntity::getDungeonCode, dungeonCode)
                .last("limit 1"));
        if (dungeon == null || isDeleted(dungeon.getDeleteFlag())) {
            throw new BizException(ErrorCode.NOT_FOUND, "副本不存在");
        }
        return dungeon;
    }

    private EncounterDefEntity requireEncounter(String encounterCode) {
        EncounterDefEntity encounter = encounterDefMapper.selectOne(new LambdaQueryWrapper<EncounterDefEntity>()
                .eq(EncounterDefEntity::getEncounterCode, encounterCode)
                .last("limit 1"));
        if (encounter == null || isDeleted(encounter.getDeleteFlag())) {
            throw new BizException(ErrorCode.NOT_FOUND, "遭遇不存在");
        }
        return encounter;
    }

    private List<EncounterWaveEntity> requireWaves(Long encounterId) {
        List<EncounterWaveEntity> list = encounterWaveMapper.selectList(new LambdaQueryWrapper<EncounterWaveEntity>()
                .eq(EncounterWaveEntity::getEncounterId, encounterId)
                .orderByAsc(EncounterWaveEntity::getWaveNo));
        if (list == null || list.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "遭遇未配置波次");
        }
        return list;
    }

    private void deductEnergy(PlayerEntity player, int energyCost) {
        player.setEnergy(nullSafe(player.getEnergy()) - energyCost);
        player.setEnergyUpdateTime(LocalDateTime.now());
        int rows = playerMapper.updateById(player);
        if (rows != 1) {
            throw new BizException(ErrorCode.CONFLICT, "扣除体力失败，请重试");
        }
    }

    private List<BattleRewardItemDTO> settleWaveRewards(PlayerEntity player,
                                                        DungeonDefEntity dungeon,
                                                        EncounterWaveEntity wave,
                                                        Map<String, BattleRewardItemDTO> totalRewardMap,
                                                        Long accountId) {
        BattleRewardConfigDTO config = readJson(wave.getRewardJson(), BattleRewardConfigDTO.class);
        List<RewardEntryDTO> rolled = new ArrayList<>();
        if (config != null && config.getRolls() != null) {
            for (RewardEntryDTO roll : config.getRolls()) {
                if (hitProbability(roll)) {
                    rolled.add(roll);
                }
            }
        }
        RewardEntryDTO picked = pickOne(config == null ? null : config.getPickOneByWeight());
        if (picked != null) {
            rolled.add(picked);
        }

        Map<String, BattleRewardItemDTO> aggregated = new LinkedHashMap<>();
        for (RewardEntryDTO entry : rolled) {
            BattleRewardItemDTO reward = toReward(entry);
            mergeReward(aggregated, reward);
            mergeReward(totalRewardMap, reward);
        }

        List<BattleRewardItemDTO> rewards = new ArrayList<>(aggregated.values());
        for (BattleRewardItemDTO reward : rewards) {
            if (REWARD_TYPE_CURRENCY.equalsIgnoreCase(reward.getType())) {
                grantCurrency(player, reward, dungeon.getId(), accountId);
            } else if (REWARD_TYPE_ITEM.equalsIgnoreCase(reward.getType())) {
                grantItem(player, reward, accountId);
            }
        }
        return rewards;
    }

    private BattleRewardItemDTO toReward(RewardEntryDTO entry) {
        if (entry == null || !StringUtils.hasText(entry.getType())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "奖励配置缺少type");
        }
        BattleRewardItemDTO dto = new BattleRewardItemDTO();
        dto.setType(entry.getType().toLowerCase(Locale.ROOT));
        dto.setAmount((long) randomAmount(entry.getMin(), entry.getMax()));
        if (REWARD_TYPE_CURRENCY.equalsIgnoreCase(entry.getType())) {
            if (!StringUtils.hasText(entry.getCurrency())) {
                throw new BizException(ErrorCode.PARAM_INVALID, "货币奖励缺少currency");
            }
            dto.setCode(entry.getCurrency());
            dto.setName(entry.getCurrency());
            return dto;
        }
        if (REWARD_TYPE_ITEM.equalsIgnoreCase(entry.getType())) {
            if (!StringUtils.hasText(entry.getItemCode())) {
                throw new BizException(ErrorCode.PARAM_INVALID, "物品奖励缺少itemCode");
            }
            ItemDefEntity item = requireItemByCode(entry.getItemCode());
            dto.setCode(item.getItemCode());
            dto.setName(item.getNameZh());
            return dto;
        }
        throw new BizException(ErrorCode.PARAM_INVALID, "不支持的奖励类型: " + entry.getType());
    }

    private void grantCurrency(PlayerEntity player, BattleRewardItemDTO reward, Long dungeonId, Long accountId) {
        WalletEntity wallet = walletMapper.selectOne(new LambdaQueryWrapper<WalletEntity>()
                .eq(WalletEntity::getPlayerId, player.getId())
                .eq(WalletEntity::getCurrencyCode, reward.getCode())
                .last("limit 1"));
        if (wallet == null) {
            wallet = new WalletEntity();
            wallet.setPlayerId(player.getId());
            wallet.setServerId(player.getServerId());
            wallet.setCurrencyCode(reward.getCode());
            wallet.setBalance(0L);
            wallet.setCreateUser(String.valueOf(accountId));
            wallet.setUpdateUser(String.valueOf(accountId));
            walletMapper.insert(wallet);
        }

        wallet.setBalance(nullSafe(wallet.getBalance()) + reward.getAmount());
        wallet.setUpdateUser(String.valueOf(accountId));
        int rows = walletMapper.updateById(wallet);
        if (rows != 1) {
            throw new BizException(ErrorCode.CONFLICT, "钱包更新失败，请重试");
        }

        WalletFlowEntity flow = new WalletFlowEntity();
        flow.setPlayerId(player.getId());
        flow.setServerId(player.getServerId());
        flow.setCurrencyCode(reward.getCode());
        flow.setDelta(reward.getAmount());
        flow.setBalanceAfter(wallet.getBalance());
        flow.setReason(WALLET_REASON_DUNGEON);
        flow.setRefType(WALLET_REF_TYPE_DUNGEON);
        flow.setRefId(dungeonId);
        flow.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        flow.setFlowTime(LocalDateTime.now());
        flow.setRemarks("battle wave reward");
        flow.setCreateUser(String.valueOf(accountId));
        flow.setUpdateUser(String.valueOf(accountId));
        walletFlowMapper.insert(flow);
    }

    private void grantItem(PlayerEntity player, BattleRewardItemDTO reward, Long accountId) {
        ItemDefEntity item = requireItemByCode(reward.getCode());
        PlayerItemStackEntity stack = playerItemStackMapper.selectOne(new LambdaQueryWrapper<PlayerItemStackEntity>()
                .eq(PlayerItemStackEntity::getPlayerId, player.getId())
                .eq(PlayerItemStackEntity::getItemId, item.getId())
                .eq(PlayerItemStackEntity::getLocation, ITEM_LOCATION_BAG)
                .last("limit 1"));
        if (stack == null) {
            stack = new PlayerItemStackEntity();
            stack.setPlayerId(player.getId());
            stack.setServerId(player.getServerId());
            stack.setItemId(item.getId());
            stack.setQty(reward.getAmount());
            stack.setLocation(ITEM_LOCATION_BAG);
            stack.setCreateUser(String.valueOf(accountId));
            stack.setUpdateUser(String.valueOf(accountId));
            playerItemStackMapper.insert(stack);
            return;
        }

        stack.setQty(nullSafe(stack.getQty()) + reward.getAmount());
        stack.setUpdateUser(String.valueOf(accountId));
        int rows = playerItemStackMapper.updateById(stack);
        if (rows != 1) {
            throw new BizException(ErrorCode.CONFLICT, "背包更新失败，请重试");
        }
    }

    private ItemDefEntity requireItemByCode(String itemCode) {
        ItemDefEntity item = itemDefMapper.selectOne(new LambdaQueryWrapper<ItemDefEntity>()
                .eq(ItemDefEntity::getItemCode, itemCode)
                .last("limit 1"));
        if (item == null || isDeleted(item.getDeleteFlag())) {
            throw new BizException(ErrorCode.NOT_FOUND, "物品不存在: " + itemCode);
        }
        return item;
    }

    private void insertRunLog(PlayerEntity player,
                              DungeonDefEntity dungeon,
                              int stage,
                              int energyCost,
                              List<BattleRewardItemDTO> totalRewards,
                              List<BattleWaveResultDTO> waveResults,
                              Long accountId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("totalRewards", totalRewards);
        payload.put("waves", waveResults);

        DungeonRunLogEntity log = new DungeonRunLogEntity();
        log.setPlayerId(player.getId());
        log.setServerId(player.getServerId());
        log.setDungeonId(dungeon.getId());
        log.setStage(stage);
        log.setResult(RUN_RESULT_SUCCESS);
        log.setEnergyCost(energyCost);
        log.setBattleTimeMs(dungeon.getBattleTimeMs());
        log.setRewardJson(writeJson(payload));
        log.setSeed(ThreadLocalRandom.current().nextLong());
        log.setRunTime(LocalDateTime.now());
        log.setRemarks("battle start success");
        log.setCreateUser(String.valueOf(accountId));
        log.setUpdateUser(String.valueOf(accountId));
        dungeonRunLogMapper.insert(log);
    }

    private void upsertProgress(PlayerEntity player, DungeonDefEntity dungeon, int bestStage, Long accountId) {
        PlayerDungeonProgressEntity progress = playerDungeonProgressMapper.selectOne(new LambdaQueryWrapper<PlayerDungeonProgressEntity>()
                .eq(PlayerDungeonProgressEntity::getPlayerId, player.getId())
                .eq(PlayerDungeonProgressEntity::getDungeonId, dungeon.getId())
                .last("limit 1"));
        if (progress == null) {
            progress = new PlayerDungeonProgressEntity();
            progress.setPlayerId(player.getId());
            progress.setServerId(player.getServerId());
            progress.setDungeonId(dungeon.getId());
            progress.setBestStage(bestStage);
            progress.setBestTimeMs(dungeon.getBattleTimeMs());
            progress.setTotalRuns(1L);
            progress.setCreateUser(String.valueOf(accountId));
            progress.setUpdateUser(String.valueOf(accountId));
            playerDungeonProgressMapper.insert(progress);
            return;
        }

        progress.setBestStage(Math.max(nullSafe(progress.getBestStage()), bestStage));
        if (progress.getBestTimeMs() == null || progress.getBestTimeMs() == 0 || dungeon.getBattleTimeMs() < progress.getBestTimeMs()) {
            progress.setBestTimeMs(dungeon.getBattleTimeMs());
        }
        progress.setTotalRuns(nullSafe(progress.getTotalRuns()) + 1L);
        progress.setUpdateUser(String.valueOf(accountId));
        int rows = playerDungeonProgressMapper.updateById(progress);
        if (rows != 1) {
            throw new BizException(ErrorCode.CONFLICT, "副本进度更新失败，请重试");
        }
    }

    private boolean hitProbability(RewardEntryDTO entry) {
        int prob = entry.getProbPerMillion() == null ? 1_000_000 : entry.getProbPerMillion();
        if (prob <= 0) {
            return false;
        }
        if (prob >= 1_000_000) {
            return true;
        }
        return ThreadLocalRandom.current().nextInt(1_000_000) < prob;
    }

    private RewardEntryDTO pickOne(List<RewardEntryDTO> entries) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        int totalWeight = 0;
        for (RewardEntryDTO entry : entries) {
            int weight = entry.getWeight() == null ? 0 : entry.getWeight();
            if (weight > 0) {
                totalWeight += weight;
            }
        }
        if (totalWeight <= 0) {
            return null;
        }
        int roll = ThreadLocalRandom.current().nextInt(totalWeight) + 1;
        int current = 0;
        for (RewardEntryDTO entry : entries) {
            int weight = entry.getWeight() == null ? 0 : entry.getWeight();
            if (weight <= 0) {
                continue;
            }
            current += weight;
            if (roll <= current) {
                return entry;
            }
        }
        return null;
    }

    private int randomAmount(Integer min, Integer max) {
        int actualMin = min == null ? 1 : min;
        int actualMax = max == null ? actualMin : max;
        if (actualMax < actualMin) {
            int tmp = actualMin;
            actualMin = actualMax;
            actualMax = tmp;
        }
        if (actualMax == actualMin) {
            return actualMin;
        }
        return ThreadLocalRandom.current().nextInt(actualMin, actualMax + 1);
    }

    private void mergeReward(Map<String, BattleRewardItemDTO> target, BattleRewardItemDTO reward) {
        String key = reward.getType() + ":" + reward.getCode();
        BattleRewardItemDTO exists = target.get(key);
        if (exists == null) {
            BattleRewardItemDTO copy = new BattleRewardItemDTO();
            copy.setType(reward.getType());
            copy.setCode(reward.getCode());
            copy.setName(reward.getName());
            copy.setAmount(reward.getAmount());
            target.put(key, copy);
            return;
        }
        exists.setAmount(nullSafe(exists.getAmount()) + reward.getAmount());
    }

    private <T> T readJson(String json, Class<T> clazz) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "JSON解析失败: " + e.getMessage());
        }
    }

    private String writeJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "JSON序列化失败: " + e.getMessage());
        }
    }

    private boolean isDeleted(Integer deleteFlag) {
        return deleteFlag != null && deleteFlag == 1;
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }
}
