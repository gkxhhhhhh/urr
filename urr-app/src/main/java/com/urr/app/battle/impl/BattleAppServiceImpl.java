package com.urr.app.battle.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.urr.app.battle.BattleAppService;
import com.urr.commons.exception.BizException;
import com.urr.commons.exception.ErrorCode;
import com.urr.domain.action.ActionDefEntity;
import com.urr.domain.battle.DungeonDefEntity;
import com.urr.domain.battle.DungeonRunLogEntity;
import com.urr.domain.battle.EncounterDefEntity;
import com.urr.domain.battle.EncounterWaveEntity;
import com.urr.domain.battle.PlayerDungeonProgressEntity;
import com.urr.domain.battle.dto.ActionParamsDTO;
import com.urr.domain.battle.dto.BattleNodeDetailDTO;
import com.urr.domain.battle.dto.BattleRewardConfigDTO;
import com.urr.domain.battle.dto.BattleRewardItemDTO;
import com.urr.domain.battle.dto.BattleStartResultDTO;
import com.urr.domain.battle.dto.BattleWaveResultDTO;
import com.urr.domain.battle.dto.BattleWorkspaceAreaDTO;
import com.urr.domain.battle.dto.BattleWorkspaceCardDTO;
import com.urr.domain.battle.dto.BattleWorkspaceDTO;
import com.urr.domain.battle.dto.BattleWorkspaceTabDTO;
import com.urr.domain.battle.dto.DungeonMetaDTO;
import com.urr.domain.battle.dto.LastBattleResultDTO;
import com.urr.domain.battle.dto.RewardEntryDTO;
import com.urr.domain.item.ItemDefEntity;
import com.urr.domain.item.PlayerItemStackEntity;
import com.urr.domain.player.PlayerEntity;
import com.urr.domain.wallet.WalletEntity;
import com.urr.domain.wallet.WalletFlowEntity;
import com.urr.infra.mapper.ActionDefMapper;
import com.urr.infra.mapper.DungeonDefMapper;
import com.urr.infra.mapper.DungeonRunLogMapper;
import com.urr.infra.mapper.EncounterDefMapper;
import com.urr.infra.mapper.EncounterWaveMapper;
import com.urr.infra.mapper.ItemDefMapper;
import com.urr.infra.mapper.PlayerDungeonProgressMapper;
import com.urr.infra.mapper.PlayerItemStackMapper;
import com.urr.infra.mapper.PlayerMapper;
import com.urr.infra.mapper.WalletFlowMapper;
import com.urr.infra.mapper.WalletMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 战斗应用服务实现。
 *
 * 说明：
 * 1. 保留当前仓库已有的战斗结算逻辑。
 * 2. 本次只做战斗查询接口收口和 start 结果增强。
 * 3. 不扩散到首页快照、市场、职业动作等已完成模块。
 */
@Service
@RequiredArgsConstructor
public class BattleAppServiceImpl implements BattleAppService {

    /**
     * JSON 工具。
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * 即时动作类型。
     */
    private static final String ACTION_KIND_INSTANT = "INSTANT";

    /**
     * 货币奖励类型。
     */
    private static final String REWARD_TYPE_CURRENCY = "currency";

    /**
     * 物品奖励类型。
     */
    private static final String REWARD_TYPE_ITEM = "item";

    /**
     * 钱包流水原因。
     */
    private static final String WALLET_REASON_DUNGEON = "DUNGEON";

    /**
     * 钱包流水关联类型。
     */
    private static final String WALLET_REF_TYPE_DUNGEON = "DUNGEON";

    /**
     * 背包位置：普通背包。
     */
    private static final int ITEM_LOCATION_BAG = 1;

    /**
     * 运行结果：成功。
     */
    private static final int RUN_RESULT_SUCCESS = 1;

    /**
     * 玩家 Mapper。
     */
    private final PlayerMapper playerMapper;

    /**
     * 动作定义 Mapper。
     */
    private final ActionDefMapper actionDefMapper;

    /**
     * 副本定义 Mapper。
     */
    private final DungeonDefMapper dungeonDefMapper;

    /**
     * 遭遇定义 Mapper。
     */
    private final EncounterDefMapper encounterDefMapper;

    /**
     * 遭遇波次 Mapper。
     */
    private final EncounterWaveMapper encounterWaveMapper;

    /**
     * 物品定义 Mapper。
     */
    private final ItemDefMapper itemDefMapper;

    /**
     * 钱包 Mapper。
     */
    private final WalletMapper walletMapper;

    /**
     * 钱包流水 Mapper。
     */
    private final WalletFlowMapper walletFlowMapper;

    /**
     * 玩家物品堆叠 Mapper。
     */
    private final PlayerItemStackMapper playerItemStackMapper;

    /**
     * 战斗日志 Mapper。
     */
    private final DungeonRunLogMapper dungeonRunLogMapper;

    /**
     * 玩家副本进度 Mapper。
     */
    private final PlayerDungeonProgressMapper playerDungeonProgressMapper;

    /**
     * 查询战斗工作区轻量快照。
     *
     * @param accountId 账号ID
     * @param playerId  玩家ID
     * @return 战斗工作区
     */
    @Override
    public BattleWorkspaceDTO loadWorkspace(Long accountId, Long playerId) {
        validateLogin(accountId, playerId);

        PlayerEntity player = requirePlayer(playerId);
        checkPlayerOwnership(accountId, player);

        BattleWorkspaceDTO dto = new BattleWorkspaceDTO();
        dto.setTitle("战斗");

        BattleWorkspaceTabDTO tab = new BattleWorkspaceTabDTO();
        tab.setCode("ALL");
        tab.setName("全部");
        dto.getTabs().add(tab);

        BattleWorkspaceAreaDTO area = new BattleWorkspaceAreaDTO();
        area.setCode("ALL");
        area.setName("全部");
        dto.getAreas().add(area);

        List<ActionDefEntity> actions = actionDefMapper.selectList(new LambdaQueryWrapper<ActionDefEntity>()
                .eq(ActionDefEntity::getStatus, 1)
                .eq(ActionDefEntity::getActionKind, ACTION_KIND_INSTANT)
                .orderByAsc(ActionDefEntity::getId));

        boolean selectedAssigned = false;
        if (actions == null || actions.isEmpty()) {
            return dto;
        }

        for (ActionDefEntity action : actions) {
            if (action == null || isDeleted(action.getDeleteFlag())) {
                continue;
            }

            ActionParamsDTO actionParams = readJson(action.getParamsJson(), ActionParamsDTO.class);
            if (actionParams == null || !StringUtils.hasText(actionParams.getDungeonCode())) {
                continue;
            }

            DungeonDefEntity dungeon = findDungeon(actionParams.getDungeonCode());
            if (dungeon == null) {
                continue;
            }

            int requiredLevel = Math.max(nullSafe(action.getMinPlayerLevel()), nullSafe(dungeon.getMinLevel()));
            int energyCost = dungeon.getEnergyCost() != null ? dungeon.getEnergyCost() : nullSafe(action.getBaseEnergyCost());

            BattleWorkspaceCardDTO card = new BattleWorkspaceCardDTO();
            card.setCode(action.getActionCode());
            card.setName(StringUtils.hasText(action.getActionName()) ? action.getActionName() : dungeon.getNameZh());
            card.setActionKind(action.getActionKind());
            card.setMinLevel(requiredLevel);
            card.setCostStamina(energyCost);
            card.setDisabled(nullSafe(player.getLevel()) < requiredLevel || nullSafe(player.getEnergy()) < energyCost);

            if (!selectedAssigned) {
                card.setSelected(Boolean.TRUE);
                selectedAssigned = true;
            } else {
                card.setSelected(Boolean.FALSE);
            }

            dto.getCards().add(card);
        }

        return dto;
    }

    /**
     * 查询单个战斗节点详情。
     *
     * @param accountId  账号ID
     * @param playerId   玩家ID
     * @param actionCode 动作编码
     * @return 节点详情
     */
    @Override
    public BattleNodeDetailDTO loadNodeDetail(Long accountId, Long playerId, String actionCode) {
        validateLogin(accountId, playerId);

        PlayerEntity player = requirePlayer(playerId);
        checkPlayerOwnership(accountId, player);

        ActionDefEntity action = requireAction(actionCode);
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
        int energyCost = dungeon.getEnergyCost() != null ? dungeon.getEnergyCost() : nullSafe(action.getBaseEnergyCost());
        boolean disabled = nullSafe(player.getLevel()) < requiredLevel || nullSafe(player.getEnergy()) < energyCost;

        PlayerDungeonProgressEntity progress = findProgress(player.getId(), dungeon.getId());

        BattleNodeDetailDTO dto = new BattleNodeDetailDTO();
        dto.setPlayerId(player.getId());
        dto.setActionCode(action.getActionCode());
        dto.setDungeonCode(dungeon.getDungeonCode());
        dto.setEncounterCode(encounter.getEncounterCode());
        dto.setName(StringUtils.hasText(action.getActionName()) ? action.getActionName() : dungeon.getNameZh());
        dto.setMinLevel(requiredLevel);
        dto.setEnergyCost(energyCost);
        dto.setWaveCount(waves.size());
        dto.setChallengeable(!disabled);
        dto.setDisabled(disabled);
        dto.setCurrentEnergy(nullSafe(player.getEnergy()));
        dto.setCurrentLevel(nullSafe(player.getLevel()));
        dto.setBestStage(progress == null ? 0 : nullSafe(progress.getBestStage()));
        dto.setTotalRuns(progress == null ? 0L : nullSafe(progress.getTotalRuns()));
        return dto;
    }

    /**
     * 查询最近一次战斗结果。
     *
     * @param accountId 账号ID
     * @param playerId  玩家ID
     * @return 最近一次战斗结果
     */
    @Override
    public LastBattleResultDTO queryLastBattleResult(Long accountId, Long playerId) {
        validateLogin(accountId, playerId);

        PlayerEntity player = requirePlayer(playerId);
        checkPlayerOwnership(accountId, player);

        DungeonRunLogEntity log = dungeonRunLogMapper.selectOne(new LambdaQueryWrapper<DungeonRunLogEntity>()
                .eq(DungeonRunLogEntity::getPlayerId, player.getId())
                .orderByDesc(DungeonRunLogEntity::getId)
                .last("limit 1"));

        LastBattleResultDTO dto = new LastBattleResultDTO();
        dto.setPlayerId(player.getId());

        if (log == null || isDeleted(log.getDeleteFlag())) {
            dto.setResult("NONE");
            return dto;
        }

        DungeonDefEntity dungeon = dungeonDefMapper.selectById(log.getDungeonId());
        BattleRunLogPayload payload = readJson(log.getRewardJson(), BattleRunLogPayload.class);

        List<BattleRewardItemDTO> totalRewards = payload == null || payload.getTotalRewards() == null
                ? new ArrayList<>()
                : payload.getTotalRewards();
        List<BattleWaveResultDTO> waves = payload == null || payload.getWaves() == null
                ? new ArrayList<>()
                : payload.getWaves();

        normalizeRewards(totalRewards);
        normalizeWaveRewards(waves);

        dto.setRunId(log.getId());
        dto.setDungeonCode(dungeon == null ? null : dungeon.getDungeonCode());
        dto.setResult(log.getResult() != null && log.getResult() == RUN_RESULT_SUCCESS ? "SUCCESS" : "FAIL");
        dto.setEnergyCost(log.getEnergyCost());
        dto.setBattleTimeMs(log.getBattleTimeMs());
        dto.setTotalRewards(totalRewards);
        dto.setWaves(waves);
        return dto;
    }

    /**
     * 发起一次战斗。
     *
     * @param accountId  账号ID
     * @param playerId   玩家ID
     * @param actionCode 动作编码
     * @return 战斗结果
     */
    @Override
    @Transactional
    public BattleStartResultDTO startBattle(Long accountId, Long playerId, String actionCode) {
        validateLogin(accountId, playerId);

        PlayerEntity player = requirePlayer(playerId);
        checkPlayerOwnership(accountId, player);

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
        normalizeRewards(totalRewards);
        normalizeWaveRewards(waveResults);

        Long runId = insertRunLog(player, dungeon, waves.size(), energyCost, totalRewards, waveResults, accountId);
        upsertProgress(player, dungeon, waves.size(), accountId);

        BattleStartResultDTO result = new BattleStartResultDTO();
        result.setRunId(runId);
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

    /**
     * 校验登录态与基础参数。
     *
     * @param accountId 账号ID
     * @param playerId  玩家ID
     */
    private void validateLogin(Long accountId, Long playerId) {
        if (accountId == null) {
            throw new IllegalArgumentException("未登录");
        }
        if (playerId == null) {
            throw new IllegalArgumentException("playerId不能为空");
        }
    }

    /**
     * 校验角色归属。
     *
     * @param accountId 账号ID
     * @param player    玩家实体
     */
    private void checkPlayerOwnership(Long accountId, PlayerEntity player) {
        if (!accountId.equals(player.getAccountId())) {
            throw new BizException(ErrorCode.CONFLICT, "无权限操作该角色");
        }
    }

    /**
     * 必须读取到玩家。
     *
     * @param playerId 玩家ID
     * @return 玩家实体
     */
    private PlayerEntity requirePlayer(Long playerId) {
        PlayerEntity player = playerMapper.selectById(playerId);
        if (player == null || isDeleted(player.getDeleteFlag())) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return player;
    }

    /**
     * 必须读取到动作。
     *
     * @param actionCode 动作编码
     * @return 动作实体
     */
    private ActionDefEntity requireAction(String actionCode) {
        if (!StringUtils.hasText(actionCode)) {
            throw new IllegalArgumentException("actionCode不能为空");
        }

        ActionDefEntity action = actionDefMapper.selectOne(new LambdaQueryWrapper<ActionDefEntity>()
                .eq(ActionDefEntity::getActionCode, actionCode)
                .last("limit 1"));

        if (action == null || isDeleted(action.getDeleteFlag())) {
            throw new BizException(ErrorCode.NOT_FOUND, "动作不存在");
        }
        return action;
    }

    /**
     * 校验动作是否可作为即时战斗入口。
     *
     * @param action 动作实体
     */
    private void validateAction(ActionDefEntity action) {
        if (action.getStatus() == null || action.getStatus() != 1) {
            throw new BizException(ErrorCode.CONFLICT, "动作未启用");
        }
        if (!ACTION_KIND_INSTANT.equalsIgnoreCase(action.getActionKind())) {
            throw new BizException(ErrorCode.CONFLICT, "当前动作不是即时战斗入口");
        }
    }

    /**
     * 必须读取到副本。
     *
     * @param dungeonCode 副本编码
     * @return 副本实体
     */
    private DungeonDefEntity requireDungeon(String dungeonCode) {
        DungeonDefEntity dungeon = findDungeon(dungeonCode);
        if (dungeon == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "副本不存在");
        }
        return dungeon;
    }

    /**
     * 尝试读取副本。
     *
     * @param dungeonCode 副本编码
     * @return 副本实体
     */
    private DungeonDefEntity findDungeon(String dungeonCode) {
        if (!StringUtils.hasText(dungeonCode)) {
            return null;
        }

        DungeonDefEntity dungeon = dungeonDefMapper.selectOne(new LambdaQueryWrapper<DungeonDefEntity>()
                .eq(DungeonDefEntity::getDungeonCode, dungeonCode)
                .last("limit 1"));

        if (dungeon == null || isDeleted(dungeon.getDeleteFlag())) {
            return null;
        }
        return dungeon;
    }

    /**
     * 必须读取到遭遇。
     *
     * @param encounterCode 遭遇编码
     * @return 遭遇实体
     */
    private EncounterDefEntity requireEncounter(String encounterCode) {
        EncounterDefEntity encounter = encounterDefMapper.selectOne(new LambdaQueryWrapper<EncounterDefEntity>()
                .eq(EncounterDefEntity::getEncounterCode, encounterCode)
                .last("limit 1"));

        if (encounter == null || isDeleted(encounter.getDeleteFlag())) {
            throw new BizException(ErrorCode.NOT_FOUND, "遭遇不存在");
        }
        return encounter;
    }

    /**
     * 必须读取到遭遇波次。
     *
     * @param encounterId 遭遇ID
     * @return 波次列表
     */
    private List<EncounterWaveEntity> requireWaves(Long encounterId) {
        List<EncounterWaveEntity> list = encounterWaveMapper.selectList(new LambdaQueryWrapper<EncounterWaveEntity>()
                .eq(EncounterWaveEntity::getEncounterId, encounterId)
                .orderByAsc(EncounterWaveEntity::getWaveNo));

        if (list == null || list.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "遭遇未配置波次");
        }
        return list;
    }

    /**
     * 查询玩家副本进度。
     *
     * @param playerId  玩家ID
     * @param dungeonId 副本ID
     * @return 副本进度
     */
    private PlayerDungeonProgressEntity findProgress(Long playerId, Long dungeonId) {
        return playerDungeonProgressMapper.selectOne(new LambdaQueryWrapper<PlayerDungeonProgressEntity>()
                .eq(PlayerDungeonProgressEntity::getPlayerId, playerId)
                .eq(PlayerDungeonProgressEntity::getDungeonId, dungeonId)
                .last("limit 1"));
    }

    /**
     * 扣除体力。
     *
     * @param player     玩家实体
     * @param energyCost 体力消耗
     */
    private void deductEnergy(PlayerEntity player, int energyCost) {
        player.setEnergy(nullSafe(player.getEnergy()) - energyCost);
        player.setEnergyUpdateTime(LocalDateTime.now());

        int rows = playerMapper.updateById(player);
        if (rows != 1) {
            throw new BizException(ErrorCode.CONFLICT, "扣除体力失败，请重试");
        }
    }

    /**
     * 结算单波奖励。
     *
     * @param player         玩家实体
     * @param dungeon        副本实体
     * @param wave           波次实体
     * @param totalRewardMap 总奖励聚合
     * @param accountId      账号ID
     * @return 当前波奖励列表
     */
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
        normalizeRewards(rewards);

        for (BattleRewardItemDTO reward : rewards) {
            if (REWARD_TYPE_CURRENCY.equalsIgnoreCase(reward.getType())) {
                grantCurrency(player, reward, dungeon.getId(), accountId);
            } else if (REWARD_TYPE_ITEM.equalsIgnoreCase(reward.getType())) {
                grantItem(player, reward, accountId);
            }
        }
        return rewards;
    }

    /**
     * 将奖励配置转换为奖励 DTO。
     *
     * @param entry 奖励配置项
     * @return 奖励 DTO
     */
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
            dto.setCurrency(entry.getCurrency());
            dto.setCode(entry.getCurrency());
            dto.setName(entry.getCurrency());
            return dto;
        }

        if (REWARD_TYPE_ITEM.equalsIgnoreCase(entry.getType())) {
            if (!StringUtils.hasText(entry.getItemCode())) {
                throw new BizException(ErrorCode.PARAM_INVALID, "物品奖励缺少itemCode");
            }

            ItemDefEntity item = requireItemByCode(entry.getItemCode());
            dto.setItemCode(item.getItemCode());
            dto.setCode(item.getItemCode());
            dto.setName(item.getNameZh());
            return dto;
        }

        throw new BizException(ErrorCode.PARAM_INVALID, "不支持的奖励类型: " + entry.getType());
    }

    /**
     * 发放货币。
     *
     * @param player    玩家实体
     * @param reward    奖励
     * @param dungeonId 副本ID
     * @param accountId 账号ID
     */
    private void grantCurrency(PlayerEntity player, BattleRewardItemDTO reward, Long dungeonId, Long accountId) {
        String currencyCode = StringUtils.hasText(reward.getCurrency()) ? reward.getCurrency() : reward.getCode();

        WalletEntity wallet = walletMapper.selectOne(new LambdaQueryWrapper<WalletEntity>()
                .eq(WalletEntity::getPlayerId, player.getId())
                .eq(WalletEntity::getCurrencyCode, currencyCode)
                .last("limit 1"));

        if (wallet == null) {
            wallet = new WalletEntity();
            wallet.setPlayerId(player.getId());
            wallet.setServerId(player.getServerId());
            wallet.setCurrencyCode(currencyCode);
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
        flow.setCurrencyCode(currencyCode);
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

    /**
     * 发放物品。
     *
     * @param player    玩家实体
     * @param reward    奖励
     * @param accountId 账号ID
     */
    private void grantItem(PlayerEntity player, BattleRewardItemDTO reward, Long accountId) {
        String itemCode = StringUtils.hasText(reward.getItemCode()) ? reward.getItemCode() : reward.getCode();
        ItemDefEntity item = requireItemByCode(itemCode);

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

    /**
     * 必须读取到物品定义。
     *
     * @param itemCode 物品编码
     * @return 物品实体
     */
    private ItemDefEntity requireItemByCode(String itemCode) {
        ItemDefEntity item = itemDefMapper.selectOne(new LambdaQueryWrapper<ItemDefEntity>()
                .eq(ItemDefEntity::getItemCode, itemCode)
                .last("limit 1"));

        if (item == null || isDeleted(item.getDeleteFlag())) {
            throw new BizException(ErrorCode.NOT_FOUND, "物品不存在: " + itemCode);
        }
        return item;
    }

    /**
     * 写入战斗日志。
     *
     * @param player       玩家实体
     * @param dungeon      副本实体
     * @param stage        通关阶段
     * @param energyCost   体力消耗
     * @param totalRewards 总奖励
     * @param waveResults  波次结果
     * @param accountId    账号ID
     * @return 日志主键ID
     */
    private Long insertRunLog(PlayerEntity player,
                              DungeonDefEntity dungeon,
                              int stage,
                              int energyCost,
                              List<BattleRewardItemDTO> totalRewards,
                              List<BattleWaveResultDTO> waveResults,
                              Long accountId) {
        BattleRunLogPayload payload = new BattleRunLogPayload();
        payload.setTotalRewards(totalRewards);
        payload.setWaves(waveResults);

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
        return log.getId();
    }

    /**
     * 回写玩家副本进度。
     *
     * @param player     玩家实体
     * @param dungeon    副本实体
     * @param bestStage  最佳阶段
     * @param accountId  账号ID
     */
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

    /**
     * 判断概率命中。
     *
     * @param entry 奖励配置项
     * @return 是否命中
     */
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

    /**
     * 按权重抽取一个奖励项。
     *
     * @param entries 奖励配置列表
     * @return 命中的奖励项
     */
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

    /**
     * 生成奖励数量。
     *
     * @param min 最小值
     * @param max 最大值
     * @return 最终数量
     */
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

    /**
     * 聚合奖励。
     *
     * @param target 目标映射
     * @param reward 当前奖励
     */
    private void mergeReward(Map<String, BattleRewardItemDTO> target, BattleRewardItemDTO reward) {
        String rewardCode = StringUtils.hasText(reward.getCurrency()) ? reward.getCurrency() : reward.getItemCode();
        if (!StringUtils.hasText(rewardCode)) {
            rewardCode = reward.getCode();
        }

        String key = reward.getType() + ":" + rewardCode;
        BattleRewardItemDTO exists = target.get(key);
        if (exists == null) {
            BattleRewardItemDTO copy = new BattleRewardItemDTO();
            copy.setType(reward.getType());
            copy.setCurrency(reward.getCurrency());
            copy.setItemCode(reward.getItemCode());
            copy.setCode(reward.getCode());
            copy.setName(reward.getName());
            copy.setAmount(reward.getAmount());
            target.put(key, copy);
            return;
        }

        exists.setAmount(nullSafe(exists.getAmount()) + reward.getAmount());
    }

    /**
     * 规范化奖励列表。
     *
     * @param rewards 奖励列表
     */
    private void normalizeRewards(List<BattleRewardItemDTO> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            return;
        }

        for (BattleRewardItemDTO reward : rewards) {
            normalizeReward(reward);
        }
    }

    /**
     * 规范化波次奖励列表。
     *
     * @param waves 波次结果列表
     */
    private void normalizeWaveRewards(List<BattleWaveResultDTO> waves) {
        if (waves == null || waves.isEmpty()) {
            return;
        }

        for (BattleWaveResultDTO wave : waves) {
            if (wave != null && wave.getRewards() != null) {
                normalizeRewards(wave.getRewards());
            }
        }
    }

    /**
     * 规范化单个奖励对象。
     *
     * @param reward 奖励对象
     */
    private void normalizeReward(BattleRewardItemDTO reward) {
        if (reward == null || !StringUtils.hasText(reward.getType())) {
            return;
        }

        if (REWARD_TYPE_CURRENCY.equalsIgnoreCase(reward.getType())) {
            if (!StringUtils.hasText(reward.getCurrency())) {
                reward.setCurrency(reward.getCode());
            }
            if (!StringUtils.hasText(reward.getCode())) {
                reward.setCode(reward.getCurrency());
            }
            if (!StringUtils.hasText(reward.getName())) {
                reward.setName(reward.getCurrency());
            }
            return;
        }

        if (REWARD_TYPE_ITEM.equalsIgnoreCase(reward.getType())) {
            if (!StringUtils.hasText(reward.getItemCode())) {
                reward.setItemCode(reward.getCode());
            }
            if (!StringUtils.hasText(reward.getCode())) {
                reward.setCode(reward.getItemCode());
            }
        }
    }

    /**
     * 读取 JSON。
     *
     * @param json  JSON文本
     * @param clazz 目标类型
     * @param <T>   类型参数
     * @return 解析结果
     */
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

    /**
     * 写出 JSON。
     *
     * @param value 对象
     * @return JSON 文本
     */
    private String writeJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "JSON序列化失败: " + e.getMessage());
        }
    }

    /**
     * 判断逻辑删除。
     *
     * @param deleteFlag 删除标记
     * @return 是否删除
     */
    private boolean isDeleted(Integer deleteFlag) {
        return deleteFlag != null && deleteFlag == 1;
    }

    /**
     * 空安全 Integer。
     *
     * @param value 值
     * @return 非空值
     */
    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 空安全 Long。
     *
     * @param value 值
     * @return 非空值
     */
    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 战斗日志奖励负载。
     */
    @Data
    private static class BattleRunLogPayload {

        /**
         * 总奖励。
         */
        private List<BattleRewardItemDTO> totalRewards = new ArrayList<>();

        /**
         * 波次结果。
         */
        private List<BattleWaveResultDTO> waves = new ArrayList<>();
    }
}