package com.urr.app.action.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.urr.domain.action.task.ActionTaskStatusEnum;
import com.urr.domain.action.task.ActionTaskStopReasonEnum;
import com.urr.domain.action.task.CraftTaskTimeSupport;
import com.urr.domain.action.task.PlayerActionTask;
import com.urr.domain.action.task.PlayerActionTaskEntity;
import com.urr.domain.action.task.PlayerCraftTask;
import com.urr.domain.item.ItemDefEntity;
import com.urr.domain.item.PlayerEquipEntity;
import com.urr.domain.item.PlayerItemStackEntity;
import com.urr.domain.skill.PlayerSkillEntity;
import com.urr.domain.wallet.WalletEntity;
import com.urr.domain.wallet.WalletFlowEntity;
import com.urr.infra.mapper.ItemDefMapper;
import com.urr.infra.mapper.PlayerActionTaskMapper;
import com.urr.infra.mapper.PlayerEquipMapper;
import com.urr.infra.mapper.PlayerItemStackMapper;
import com.urr.infra.mapper.PlayerSkillMapper;
import com.urr.infra.mapper.WalletFlowMapper;
import com.urr.infra.mapper.WalletMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 制造任务结算服务。
 */
@Service
@RequiredArgsConstructor
public class CraftTaskSettleService {

    /**
     * 根任务仓储。
     */
    private final PlayerActionTaskRepository playerActionTaskRepository;

    /**
     * 制造任务仓储。
     */
    private final PlayerCraftTaskRepository playerCraftTaskRepository;

    /**
     * 根任务 Mapper。
     */
    private final PlayerActionTaskMapper playerActionTaskMapper;

    /**
     * 玩家物品堆叠 Mapper。
     */
    private final PlayerItemStackMapper playerItemStackMapper;

    /**
     * 玩家装备实例 Mapper。
     */
    private final PlayerEquipMapper playerEquipMapper;

    /**
     * 物品定义 Mapper。
     */
    private final ItemDefMapper itemDefMapper;

    /**
     * 玩家技能 Mapper。
     */
    private final PlayerSkillMapper playerSkillMapper;

    /**
     * 钱包 Mapper。
     */
    private final WalletMapper walletMapper;

    /**
     * 钱包流水 Mapper。
     */
    private final WalletFlowMapper walletFlowMapper;

    /**
     * 库存消耗前准备服务。
     */
    private final GatherTaskInventoryConsumePrepareService gatherTaskInventoryConsumePrepareService;

    /**
     * 通用技能经验服务。
     */
    private final ProfessionSkillExpService professionSkillExpService;

    /**
     * JSON 工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * 结算制造任务到指定时刻。
     *
     * 规则：
     * 1. 开始时不预扣料。
     * 2. 每轮结束时先刷新待入账，再判断材料，再扣料/发产物/发经验。
     * 3. 当前轮成功后，立刻判断下一轮还能不能启动；如果不能，直接在当前轮结束时停掉，原因为 MATERIAL_SHORTAGE。
     *
     * @param taskId 根任务ID
     * @param settleTime 结算时刻
     * @return 最新制造任务
     */
    @Transactional(rollbackFor = Exception.class)
    public PlayerCraftTask settleTo(Long taskId, LocalDateTime settleTime) {
        if (taskId == null) {
            throw new IllegalArgumentException("taskId不能为空");
        }
        if (settleTime == null) {
            throw new IllegalArgumentException("settleTime不能为空");
        }

        PlayerActionTask rootTask = playerActionTaskRepository.findById(taskId);
        if (rootTask == null) {
            return null;
        }

        PlayerCraftTask craftTask = playerCraftTaskRepository.findByTaskId(taskId);
        if (craftTask == null) {
            return null;
        }
        if (!craftTask.isRunning()) {
            return craftTask;
        }

        while (craftTask.canSettleOneRound(settleTime)) {
            LocalDateTime roundFinishTime =
                    CraftTaskTimeSupport.fromEpochMilli(craftTask.getNextRoundFinishTime());

            RecipeSnapshot snapshot = parseRecipeSnapshot(craftTask.getRecipeSnapshotJson());

            // 先把采集待入账刷新到正式库存，再做本轮制造消耗判断。
            gatherTaskInventoryConsumePrepareService.prepareBeforeConsume(
                    craftTask.getPlayerId(),
                    roundFinishTime
            );

            if (!hasEnoughCost(craftTask.getPlayerId(), snapshot.getCostMap())) {
                stopByMaterialShortage(rootTask.getId(), roundFinishTime);
                craftTask.setStatus(ActionTaskStatusEnum.STOPPED);
                playerCraftTaskRepository.updateByTaskId(craftTask);
                playerCraftTaskRepository.updateStatusByTaskId(
                        rootTask.getId(),
                        ActionTaskStatusEnum.STOPPED
                );
                return playerCraftTaskRepository.findByTaskId(taskId);
            }

            consumeCost(craftTask.getPlayerId(), snapshot.getCostMap());

            boolean strengtheningSnapshot = isStrengtheningSnapshot(snapshot);
            boolean success = rollCraftSuccess(craftTask.getPlayerId(), snapshot);
            if (success) {
                if (strengtheningSnapshot) {
                    applyStrengtheningSuccess(craftTask.getPlayerId(), craftTask.getServerId(), snapshot);
                } else {
                    grantOutput(craftTask.getPlayerId(), craftTask.getServerId(), snapshot.getOutputMap());
                }
                grantCurrencyOutput(
                        craftTask.getPlayerId(),
                        craftTask.getServerId(),
                        rootTask.getId(),
                        roundFinishTime,
                        resolveCurrencyOutputMap(snapshot)
                );

                if (snapshot.getExpGain() != null && snapshot.getExpGain().longValue() > 0L) {
                    professionSkillExpService.applySkillExp(
                            craftTask.getPlayerId(),
                            snapshot.getSkillCode(),
                            snapshot.getExpGain()
                    );
                }
            } else if (strengtheningSnapshot) {
                applyStrengtheningFailure(craftTask.getPlayerId(), snapshot);
            }

            craftTask.setCompletedCount(craftTask.getSafeCompletedCount() + 1L);
            updateRootSettleTime(rootTask.getId(), roundFinishTime);

            if (craftTask.isFinishedByTargetCount()) {
                finishNormally(rootTask.getId(), roundFinishTime);
                craftTask.setStatus(ActionTaskStatusEnum.COMPLETED);
                playerCraftTaskRepository.updateByTaskId(craftTask);
                playerCraftTaskRepository.updateStatusByTaskId(
                        rootTask.getId(),
                        ActionTaskStatusEnum.COMPLETED
                );
                return playerCraftTaskRepository.findByTaskId(taskId);
            }

            // 当前轮成功后，立刻判断下一轮是否还能启动。
            // 不能让下一轮先空跑进度条，再在轮末因缺料失败。
            if (!canStartNextRoundImmediately(craftTask.getPlayerId(), snapshot.getCostMap())) {
                stopByMaterialShortage(rootTask.getId(), roundFinishTime);
                craftTask.setStatus(ActionTaskStatusEnum.STOPPED);
                playerCraftTaskRepository.updateByTaskId(craftTask);
                playerCraftTaskRepository.updateStatusByTaskId(
                        rootTask.getId(),
                        ActionTaskStatusEnum.STOPPED
                );
                return playerCraftTaskRepository.findByTaskId(taskId);
            }

            LocalDateTime nextFinishTime =
                    roundFinishTime.plusNanos(snapshot.getCraftTimeMs().longValue() * 1_000_000L);
            craftTask.setNextRoundFinishTime(CraftTaskTimeSupport.toEpochMilli(nextFinishTime));
            craftTask.setStatus(ActionTaskStatusEnum.RUNNING);
            playerCraftTaskRepository.updateByTaskId(craftTask);
        }

        return playerCraftTaskRepository.findByTaskId(taskId);
    }

    /**
     * 判断当前轮结算完成后，下一轮是否还能立刻启动。
     *
     * 说明：
     * 1. 这里只判断正式库存是否还能覆盖下一轮消耗。
     * 2. 不重复刷新 pending，因为本轮结算前已经刷新过一次，
     *    且制造任务本身不会产生新的 pending_reward_pool。
     *
     * @param playerId 玩家ID
     * @param costMap 下一轮消耗
     * @return true-还能继续，false-材料不足
     */
    private boolean canStartNextRoundImmediately(Long playerId, Map<Long, Long> costMap) {
        if (costMap == null || costMap.isEmpty()) {
            return true;
        }
        return hasEnoughCost(playerId, costMap);
    }

    /**
     * 解析配方快照。
     *
     * @param recipeSnapshotJson 配方快照 JSON
     * @return 快照对象
     */
    private RecipeSnapshot parseRecipeSnapshot(String recipeSnapshotJson) {
        if (recipeSnapshotJson == null || recipeSnapshotJson.trim().isEmpty()) {
            throw new IllegalStateException("制造任务缺少配方快照");
        }

        try {
            JsonNode root = objectMapper.readTree(recipeSnapshotJson);
            RecipeSnapshot snapshot = new RecipeSnapshot();
            snapshot.setRecipeCode(readText(root, "recipeCode"));
            snapshot.setSkillId(readLong(root, "skillId", 0L));
            snapshot.setSkillCode(readText(root, "skillCode"));
            snapshot.setCraftTimeMs(readInt(root, "craftTimeMs", 0));
            snapshot.setCraftLevelReq(readInt(root, "craftLevelReq", 1));
            snapshot.setExpGain(readLong(root, "expGain", 0L));
            snapshot.setCostMap(readLongMap(root.get("costMap")));
            snapshot.setOutputMap(readLongMap(root.get("outputMap")));
            snapshot.setMetaJson(readText(root, "metaJson"));
            return snapshot;
        } catch (Exception exception) {
            throw new IllegalStateException("解析制造配方快照失败", exception);
        }
    }

    /**
     * 读取文本字段。
     *
     * @param root 根节点
     * @param fieldName 字段名
     * @return 文本值
     */
    private String readText(JsonNode root, String fieldName) {
        if (root == null || fieldName == null) {
            return null;
        }
        JsonNode node = root.get(fieldName);
        return node == null || node.isNull() ? null : node.asText();
    }

    /**
     * 读取整数。
     *
     * @param root 根节点
     * @param fieldName 字段名
     * @param defaultValue 默认值
     * @return 整数
     */
    private Integer readInt(JsonNode root, String fieldName, int defaultValue) {
        if (root == null || fieldName == null) {
            return defaultValue;
        }
        JsonNode node = root.get(fieldName);
        return node == null || node.isNull() ? defaultValue : node.asInt(defaultValue);
    }

    /**
     * 读取长整数。
     *
     * @param root 根节点
     * @param fieldName 字段名
     * @param defaultValue 默认值
     * @return 长整数
     */
    private Long readLong(JsonNode root, String fieldName, long defaultValue) {
        if (root == null || fieldName == null) {
            return defaultValue;
        }
        JsonNode node = root.get(fieldName);
        return node == null || node.isNull() ? defaultValue : node.asLong(defaultValue);
    }

    /**
     * 把 JSON 对象转换成 long map。
     *
     * @param node JSON 节点
     * @return map
     */
    private Map<Long, Long> readLongMap(JsonNode node) {
        Map<Long, Long> result = new LinkedHashMap<>();
        if (node == null || node.isNull() || !node.isObject()) {
            return result;
        }

        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }

            long itemId = Long.parseLong(entry.getKey());
            long quantity = entry.getValue().asLong(0L);
            if (quantity <= 0L) {
                continue;
            }

            result.put(itemId, quantity);
        }
        return result;
    }

    /**
     * 判断本轮制造是否成功。
     *
     * @param playerId 玩家ID
     * @param snapshot 配方快照
     * @return true-成功，false-失败
     */
    private boolean rollCraftSuccess(Long playerId, RecipeSnapshot snapshot) {
        double successRate = resolveSuccessRate(playerId, snapshot);
        if (successRate <= 0D) {
            return false;
        }
        if (successRate >= 100D) {
            return true;
        }
        return Math.random() * 100D < successRate;
    }

    /**
     * 解析当前轮成功率。
     *
     * 说明：
     * 1. 只有 metaJson 配了 successRule 的配方才走动态成功率；
     * 2. 其他旧制造配方保持 100% 成功，不影响现有链路。
     *
     * @param playerId 玩家ID
     * @param snapshot 配方快照
     * @return 成功率
     */
    private double resolveSuccessRate(Long playerId, RecipeSnapshot snapshot) {
        JsonNode metaNode = parseMetaJson(snapshot == null ? null : snapshot.getMetaJson());
        if (isStrengtheningSnapshot(snapshot)) {
            return resolveStrengtheningSuccessRate(playerId, snapshot, metaNode) * 100D;
        }
        JsonNode successRuleNode = metaNode == null ? null : metaNode.get("successRule");
        if (successRuleNode == null || successRuleNode.isNull() || !successRuleNode.isObject()) {
            return 100D;
        }

        double base = readDouble(successRuleNode, "base", 50D);
        double belowDelta = readDouble(successRuleNode, "belowDelta", 2D);
        double aboveDelta = readDouble(successRuleNode, "aboveDelta", 0.5D);
        int requiredLevel = snapshot == null || snapshot.getCraftLevelReq() == null
                ? 1
                : Math.max(snapshot.getCraftLevelReq().intValue(), 1);
        int currentLevel = resolvePlayerSkillLevel(playerId, snapshot == null ? null : snapshot.getSkillId());

        double rate = base;
        if (currentLevel < requiredLevel) {
            rate = base - (requiredLevel - currentLevel) * belowDelta;
        } else if (currentLevel > requiredLevel) {
            rate = base + (currentLevel - requiredLevel) * aboveDelta;
        }

        if (rate < 0D) {
            return 0D;
        }
        if (rate > 100D) {
            return 100D;
        }
        return rate;
    }

    /**
     * 解析玩家当前技能等级。
     *
     * @param playerId 玩家ID
     * @param skillId 技能ID
     * @return 当前等级
     */
    private int resolvePlayerSkillLevel(Long playerId, Long skillId) {
        if (playerId == null || skillId == null || skillId.longValue() <= 0L) {
            return 1;
        }

        PlayerSkillEntity playerSkill = playerSkillMapper.selectOne(
                new LambdaQueryWrapper<PlayerSkillEntity>()
                        .eq(PlayerSkillEntity::getPlayerId, playerId)
                        .eq(PlayerSkillEntity::getSkillId, skillId)
                        .eq(PlayerSkillEntity::getDeleteFlag, 0)
                        .last("limit 1")
        );
        if (playerSkill == null || playerSkill.getSkillLevel() == null || playerSkill.getSkillLevel().intValue() <= 0) {
            return 1;
        }
        return playerSkill.getSkillLevel().intValue();
    }

    /**
     * 解析配方里的货币产出。
     *
     * @param snapshot 配方快照
     * @return 货币产出 map
     */
    private Map<String, Long> resolveCurrencyOutputMap(RecipeSnapshot snapshot) {
        JsonNode metaNode = parseMetaJson(snapshot == null ? null : snapshot.getMetaJson());
        if (metaNode == null) {
            return new LinkedHashMap<String, Long>();
        }
        return readStringLongMap(metaNode.get("currencyOutput"));
    }

    /**
     * 发放货币产出。
     *
     * @param playerId 玩家ID
     * @param serverId 区服ID
     * @param taskId 根任务ID
     * @param flowTime 流水时间
     * @param currencyOutputMap 货币产出
     */
    private void grantCurrencyOutput(Long playerId,
                                     Integer serverId,
                                     Long taskId,
                                     LocalDateTime flowTime,
                                     Map<String, Long> currencyOutputMap) {
        if (currencyOutputMap == null || currencyOutputMap.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Long> entry : currencyOutputMap.entrySet()) {
            String currencyCode = entry.getKey();
            Long delta = entry.getValue();
            if (currencyCode == null || currencyCode.trim().isEmpty() || delta == null || delta.longValue() <= 0L) {
                continue;
            }
            addWalletBalance(playerId, serverId, currencyCode.trim(), delta.longValue(), taskId, flowTime);
        }
    }

    /**
     * 增加钱包余额并记录流水。
     *
     * @param playerId 玩家ID
     * @param serverId 区服ID
     * @param currencyCode 币种
     * @param delta 增量
     * @param taskId 根任务ID
     * @param flowTime 流水时间
     */
    private void addWalletBalance(Long playerId,
                                  Integer serverId,
                                  String currencyCode,
                                  long delta,
                                  Long taskId,
                                  LocalDateTime flowTime) {
        if (playerId == null || delta <= 0L) {
            return;
        }

        WalletEntity wallet = walletMapper.selectOne(
                new LambdaQueryWrapper<WalletEntity>()
                        .eq(WalletEntity::getPlayerId, playerId)
                        .eq(WalletEntity::getCurrencyCode, currencyCode)
                        .eq(WalletEntity::getDeleteFlag, 0)
                        .last("limit 1")
        );

        long balanceAfter = delta;
        if (wallet == null) {
            wallet = new WalletEntity();
            wallet.setPlayerId(playerId);
            wallet.setServerId(serverId == null ? 1 : serverId);
            wallet.setCurrencyCode(currencyCode);
            wallet.setBalance(delta);
            wallet.setRemarks("craft_reward_currency");
            wallet.setCreateUser("-1");
            wallet.setUpdateUser("-1");
            walletMapper.insert(wallet);
        } else {
            long currentBalance = wallet.getBalance() == null ? 0L : wallet.getBalance().longValue();
            balanceAfter = currentBalance + delta;
            wallet.setBalance(balanceAfter);
            wallet.setUpdateUser("-1");
            walletMapper.updateById(wallet);
        }

        if (wallet.getBalance() != null) {
            balanceAfter = wallet.getBalance().longValue();
        }

        WalletFlowEntity walletFlow = new WalletFlowEntity();
        walletFlow.setPlayerId(playerId);
        walletFlow.setServerId(serverId == null ? 1 : serverId);
        walletFlow.setCurrencyCode(currencyCode);
        walletFlow.setDelta(delta);
        walletFlow.setBalanceAfter(balanceAfter);
        walletFlow.setReason("CRAFT");
        walletFlow.setRefType("ACTION_TASK");
        walletFlow.setRefId(taskId);
        walletFlow.setRequestId(null);
        walletFlow.setFlowTime(flowTime == null ? LocalDateTime.now() : flowTime);
        walletFlow.setRemarks("craft_reward_currency");
        walletFlow.setCreateUser("-1");
        walletFlow.setUpdateUser("-1");
        walletFlowMapper.insert(walletFlow);
    }

    /**
     * 判断是否强化快照。
     *
     * @param snapshot 配方快照
     * @return true-强化，false-非强化
     */
    private boolean isStrengtheningSnapshot(RecipeSnapshot snapshot) {
        JsonNode metaNode = parseMetaJson(snapshot == null ? null : snapshot.getMetaJson());
        return metaNode != null && metaNode.path("strengthening").asBoolean(false);
    }

    /**
     * 解析强化成功率。
     *
     * @param playerId 玩家ID
     * @param snapshot 配方快照
     * @param metaNode meta 节点
     * @return 0~1 成功率
     */
    private double resolveStrengtheningSuccessRate(Long playerId, RecipeSnapshot snapshot, JsonNode metaNode) {
        if (metaNode == null) {
            return 0D;
        }
        PlayerEquipEntity equip = requireStrengtheningEquip(playerId, metaNode.path("equipInstanceId").asLong(0L));
        ItemDefEntity itemDef = itemDefMapper.selectById(equip.getItemId());
        JsonNode itemMetaNode = parseMetaJson(itemDef == null ? null : itemDef.getMetaJson());
        JsonNode attrNode = parseMetaJson(equip.getAttrJson());

        int currentStrengthenLevel = readInt(attrNode, "strengthenLevel", 0);
        if (currentStrengthenLevel >= 20) {
            return 0D;
        }
        int targetLevel = currentStrengthenLevel + 1;
        double baseSuccessRate = resolveStrengtheningBaseSuccessRate(targetLevel);
        int strengtheningSkillLevel = resolvePlayerSkillLevel(playerId, snapshot == null ? null : snapshot.getSkillId());
        int itemLevel = Math.max(resolveStrengtheningItemLevel(itemMetaNode, attrNode), 1);
        int observatoryLevel = Math.max(readInt(metaNode, "observatoryLevel", 0), 0);
        double extraSuccessRate = Math.max(readDouble(metaNode, "extraSuccessRate", 0D), 0D);
        double effectiveLevel = resolveEffectiveStrengtheningLevel(strengtheningSkillLevel, metaNode);

        double correctionFactor;
        if (effectiveLevel < itemLevel) {
            correctionFactor = 0.5D + 0.5D * effectiveLevel / itemLevel + 0.0005D * observatoryLevel + extraSuccessRate;
        } else {
            correctionFactor = 1D + 0.0005D * (effectiveLevel + observatoryLevel - itemLevel) + extraSuccessRate;
        }
        double finalSuccessRate = baseSuccessRate * correctionFactor;
        if (finalSuccessRate < 0D) {
            return 0D;
        }
        if (finalSuccessRate > 1D) {
            return 1D;
        }
        return finalSuccessRate;
    }

    /**
     * 应用强化成功结果。
     *
     * @param playerId 玩家ID
     * @param serverId 区服ID
     * @param snapshot 配方快照
     */
    private void applyStrengtheningSuccess(Long playerId, Integer serverId, RecipeSnapshot snapshot) {
        JsonNode metaNode = parseMetaJson(snapshot == null ? null : snapshot.getMetaJson());
        PlayerEquipEntity equip = requireStrengtheningEquip(playerId, metaNode == null ? 0L : metaNode.path("equipInstanceId").asLong(0L));
        ItemDefEntity itemDef = itemDefMapper.selectById(equip.getItemId());
        JsonNode itemMetaNode = parseMetaJson(itemDef == null ? null : itemDef.getMetaJson());
        JsonNode attrNode = parseMetaJson(equip.getAttrJson());

        int currentStrengthenLevel = readInt(attrNode, "strengthenLevel", 0);
        if (currentStrengthenLevel >= 20) {
            return;
        }
        int increaseLevel = resolveStrengtheningIncreaseLevel(metaNode);
        int nextStrengthenLevel = currentStrengthenLevel + increaseLevel;
        if (nextStrengthenLevel > 20) {
            nextStrengthenLevel = 20;
        }

        double baseAttack = resolveEquipBaseAttack(attrNode, itemMetaNode);
        int itemLevel = resolveStrengtheningItemLevel(itemMetaNode, attrNode);
        double currentAttack = calculateStrengthenedAttack(baseAttack, nextStrengthenLevel);
        equip.setAttrJson(buildStrengthenedAttrJson(attrNode, nextStrengthenLevel, itemLevel, baseAttack, currentAttack, "SUCCESS"));
        equip.setUpdateUser("-1");
        playerEquipMapper.updateById(equip);
    }

    /**
     * 应用强化失败结果。
     *
     * @param playerId 玩家ID
     * @param snapshot 配方快照
     */
    private void applyStrengtheningFailure(Long playerId, RecipeSnapshot snapshot) {
        JsonNode metaNode = parseMetaJson(snapshot == null ? null : snapshot.getMetaJson());
        PlayerEquipEntity equip = requireStrengtheningEquip(playerId, metaNode == null ? 0L : metaNode.path("equipInstanceId").asLong(0L));
        ItemDefEntity itemDef = itemDefMapper.selectById(equip.getItemId());
        JsonNode itemMetaNode = parseMetaJson(itemDef == null ? null : itemDef.getMetaJson());
        JsonNode attrNode = parseMetaJson(equip.getAttrJson());

        double baseAttack = resolveEquipBaseAttack(attrNode, itemMetaNode);
        int itemLevel = resolveStrengtheningItemLevel(itemMetaNode, attrNode);
        equip.setAttrJson(buildStrengthenedAttrJson(attrNode, 0, itemLevel, baseAttack, baseAttack, "FAIL"));
        equip.setUpdateUser("-1");
        playerEquipMapper.updateById(equip);
    }

    /**
     * 解析强化成功后增加的等级。
     *
     * @param metaNode meta 节点
     * @return 增加强化等级
     */
    private int resolveStrengtheningIncreaseLevel(JsonNode metaNode) {
        if (metaNode == null || !metaNode.path("blessedTeaUsed").asBoolean(false)) {
            return 1;
        }
        double concentration = Math.max(readDouble(metaNode, "drinkConcentration", 0D), 0D);
        double guzzlingBonus = 1D + concentration;
        double plusTwoConditionalRate = 0.01D * guzzlingBonus;
        if (plusTwoConditionalRate > 1D) {
            plusTwoConditionalRate = 1D;
        }
        return Math.random() < plusTwoConditionalRate ? 2 : 1;
    }

    /**
     * 构建强化后的 attrJson。
     *
     * @param attrNode 原始扩展
     * @param strengthenLevel 强化等级
     * @param itemLevel 物品等级
     * @param baseAttack 基础攻击
     * @param currentAttack 当前攻击
     * @return attrJson
     */
    private String buildStrengthenedAttrJson(JsonNode attrNode,
                                             int strengthenLevel,
                                             int itemLevel,
                                             double baseAttack,
                                             double currentAttack) {
        return buildStrengthenedAttrJson(attrNode, strengthenLevel, itemLevel, baseAttack, currentAttack, null);
    }

    /**
     * 构建强化后的 attrJson。
     *
     * @param attrNode 原始扩展
     * @param strengthenLevel 强化等级
     * @param itemLevel 物品等级
     * @param baseAttack 基础攻击
     * @param currentAttack 当前攻击
     * @param lastStrengthenResult 最近一次强化结果
     * @return attrJson
     */
    private String buildStrengthenedAttrJson(JsonNode attrNode,
                                             int strengthenLevel,
                                             int itemLevel,
                                             double baseAttack,
                                             double currentAttack,
                                             String lastStrengthenResult) {
        ObjectNode node;
        if (attrNode instanceof ObjectNode) {
            node = (ObjectNode) attrNode.deepCopy();
        } else {
            node = objectMapper.createObjectNode();
        }
        node.put("strengthenLevel", strengthenLevel);
        node.put("itemLevel", itemLevel);
        node.put("baseAttack", roundNumber(baseAttack, 4));
        node.put("currentAttack", roundNumber(currentAttack, 4));
        if (lastStrengthenResult == null || lastStrengthenResult.trim().isEmpty()) {
            node.remove("lastStrengthenResult");
        } else {
            node.put("lastStrengthenResult", lastStrengthenResult.trim().toUpperCase());
        }
        return node.toString();
    }

    /**
     * 解析强化基础成功率。
     *
     * @param targetLevel 目标强化等级
     * @return 基础成功率
     */
    private double resolveStrengtheningBaseSuccessRate(int targetLevel) {
        if (targetLevel <= 1) {
            return 0.50D;
        }
        if (targetLevel <= 3) {
            return 0.45D;
        }
        if (targetLevel <= 6) {
            return 0.40D;
        }
        if (targetLevel <= 10) {
            return 0.35D;
        }
        return 0.30D;
    }

    /**
     * 解析有效强化等级。
     *
     * @param strengtheningSkillLevel 当前强化等级
     * @param metaNode meta 节点
     * @return 有效强化等级
     */
    private double resolveEffectiveStrengtheningLevel(int strengtheningSkillLevel, JsonNode metaNode) {
        String teaType = metaNode == null ? "NONE" : metaNode.path("teaType").asText("NONE").trim().toUpperCase();
        double concentration = Math.max(readDouble(metaNode, "drinkConcentration", 0D), 0D);
        double guzzlingBonus = 1D + concentration;
        double baseLevel = strengtheningSkillLevel;
        if ("NORMAL".equals(teaType)) {
            return baseLevel + 3D * guzzlingBonus;
        }
        if ("SUPER".equals(teaType)) {
            return baseLevel + 6D * guzzlingBonus;
        }
        if ("ULTRA".equals(teaType)) {
            return baseLevel + 8D * guzzlingBonus;
        }
        return baseLevel;
    }

    /**
     * 解析强化装备的物品等级。
     *
     * @param itemMetaNode 物品 meta
     * @param attrNode 装备 attr
     * @return 物品等级
     */
    private int resolveStrengtheningItemLevel(JsonNode itemMetaNode, JsonNode attrNode) {
        int attrItemLevel = readInt(attrNode, "itemLevel", 0);
        if (attrItemLevel > 0) {
            return attrItemLevel;
        }
        int itemLevel = readInt(itemMetaNode, "itemLevel", 0);
        if (itemLevel > 0) {
            return itemLevel;
        }
        int tier = readInt(itemMetaNode, "tier", 1);
        return tier <= 0 ? 1 : tier;
    }

    /**
     * 解析装备基础攻击。
     *
     * @param attrNode 装备属性节点
     * @param itemMetaNode 物品 meta 节点
     * @return 基础攻击
     */
    private double resolveEquipBaseAttack(JsonNode attrNode, JsonNode itemMetaNode) {
        double itemMetaAttack = readDouble(itemMetaNode == null ? null : itemMetaNode.path("baseAttrs"), "attack", 0D);
        double attrBaseAttack = readDouble(attrNode, "baseAttack", itemMetaAttack);
        if (attrBaseAttack > 0D) {
            return roundNumber(attrBaseAttack, 4);
        }
        if (itemMetaAttack > 0D) {
            return roundNumber(itemMetaAttack, 4);
        }
        return roundNumber(attrBaseAttack, 4);
    }

    /**
     * 根据基础攻击和强化等级计算强化后攻击。
     *
     * @param baseAttack 基础攻击
     * @param strengthenLevel 强化等级
     * @return 当前攻击
     */
    private double calculateStrengthenedAttack(double baseAttack, int strengthenLevel) {
        double value = baseAttack;
        for (int level = 1; level <= strengthenLevel; level++) {
            if (level <= 5) {
                value = value * 1.023D;
                continue;
            }
            if (level <= 10) {
                value = value * 1.029D;
                continue;
            }
            if (level <= 15) {
                value = value * 1.04D;
                continue;
            }
            value = value * 1.05D;
        }
        return roundNumber(value, 4);
    }

    /**
     * 判断物品是否装备。
     *
     * @param itemDef 物品定义
     * @return true-装备，false-非装备
     */
    private boolean isEquipmentItem(ItemDefEntity itemDef) {
        if (itemDef == null) {
            return false;
        }
        if (itemDef.getItemType() != null && itemDef.getItemType().intValue() == 4) {
            return true;
        }
        return itemDef.getStackable() != null && itemDef.getStackable().intValue() == 0;
    }

    /**
     * 发放装备产出。
     *
     * @param playerId 玩家ID
     * @param serverId 区服ID
     * @param itemDef 装备物品定义
     * @param quantity 数量
     */
    private void grantEquipOutput(Long playerId, Integer serverId, ItemDefEntity itemDef, long quantity) {
        if (itemDef == null || quantity <= 0L) {
            return;
        }
        for (long i = 0L; i < quantity; i++) {
            PlayerEquipEntity equip = new PlayerEquipEntity();
            equip.setPlayerId(playerId);
            equip.setServerId(serverId == null ? 1 : serverId);
            equip.setItemId(itemDef.getId());
            equip.setEquipSlot(null);
            equip.setLevelReq(resolveStrengtheningItemLevel(parseMetaJson(itemDef.getMetaJson()), null));
            equip.setBindType(itemDef.getBindType() == null ? 0 : itemDef.getBindType());
            equip.setBindPlayerId(null);
            equip.setDurability(100);
            equip.setState(1);
            equip.setAttrJson(buildInitialEquipAttrJson(itemDef));
            equip.setRemarks("craft_reward_equip");
            equip.setCreateUser("-1");
            equip.setUpdateUser("-1");
            playerEquipMapper.insert(equip);
        }
    }

    /**
     * 构建装备初始 attrJson。
     *
     * @param itemDef 物品定义
     * @return 初始 attrJson
     */
    private String buildInitialEquipAttrJson(ItemDefEntity itemDef) {
        JsonNode itemMetaNode = parseMetaJson(itemDef == null ? null : itemDef.getMetaJson());
        int itemLevel = resolveStrengtheningItemLevel(itemMetaNode, null);
        double baseAttack = readDouble(itemMetaNode == null ? null : itemMetaNode.path("baseAttrs"), "attack", 0D);
        ObjectNode node = objectMapper.createObjectNode();
        node.put("strengthenLevel", 0);
        node.put("itemLevel", itemLevel);
        node.put("baseAttack", roundNumber(baseAttack, 4));
        node.put("currentAttack", roundNumber(baseAttack, 4));
        return node.toString();
    }

    /**
     * 读取强化目标装备。
     *
     * @param playerId 玩家ID
     * @param equipInstanceId 装备实例ID
     * @return 装备实例
     */
    private PlayerEquipEntity requireStrengtheningEquip(Long playerId, Long equipInstanceId) {
        if (equipInstanceId == null || equipInstanceId.longValue() <= 0L) {
            throw new IllegalStateException("强化快照缺少装备实例ID");
        }
        PlayerEquipEntity equip = playerEquipMapper.selectById(equipInstanceId);
        if (equip == null) {
            throw new IllegalStateException("强化目标装备不存在，equipInstanceId=" + equipInstanceId);
        }
        if (playerId != null && !playerId.equals(equip.getPlayerId())) {
            throw new IllegalStateException("强化目标装备不属于当前角色，equipInstanceId=" + equipInstanceId);
        }
        if (equip.getState() == null || equip.getState().intValue() != 1) {
            throw new IllegalStateException("强化目标装备当前不在背包中，equipInstanceId=" + equipInstanceId);
        }
        if (equip.getDeleteFlag() != null && equip.getDeleteFlag().intValue() != 0) {
            throw new IllegalStateException("强化目标装备已删除，equipInstanceId=" + equipInstanceId);
        }
        return equip;
    }

    /**
     * 对数值做保留小数位处理。
     *
     * @param value 数值
     * @param scale 小数位数
     * @return 结果
     */
    private double roundNumber(double value, int scale) {
        double factor = Math.pow(10D, scale);
        return Math.round(value * factor) / factor;
    }

    /**
     * 解析 metaJson。
     *
     * @param metaJson 扩展 JSON
     * @return JSON 节点
     */
    private JsonNode parseMetaJson(String metaJson) {
        if (metaJson == null || metaJson.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readTree(metaJson);
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * 读取 double 字段。
     *
     * @param root 根节点
     * @param fieldName 字段名
     * @param defaultValue 默认值
     * @return double 值
     */
    private double readDouble(JsonNode root, String fieldName, double defaultValue) {
        if (root == null || fieldName == null) {
            return defaultValue;
        }
        JsonNode node = root.get(fieldName);
        return node == null || node.isNull() ? defaultValue : node.asDouble(defaultValue);
    }

    /**
     * 读取字符串到长整数的 map。
     *
     * @param root 根节点
     * @return map
     */
    private Map<String, Long> readStringLongMap(JsonNode root) {
        Map<String, Long> result = new LinkedHashMap<String, Long>();
        if (root == null || root.isNull() || !root.isObject()) {
            return result;
        }

        Iterator<Map.Entry<String, JsonNode>> iterator = root.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            long quantity = entry.getValue().asLong(0L);
            if (quantity <= 0L) {
                continue;
            }
            result.put(entry.getKey(), quantity);
        }
        return result;
    }

    /**
     * 判断材料是否足够。
     *
     * @param playerId 玩家ID
     * @param costMap 消耗 map
     * @return true-足够，false-不足
     */
    private boolean hasEnoughCost(Long playerId, Map<Long, Long> costMap) {
        if (costMap == null || costMap.isEmpty()) {
            return true;
        }

        for (Map.Entry<Long, Long> entry : costMap.entrySet()) {
            Long ownedQuantity = sumFormalQuantity(playerId, entry.getKey());
            if (ownedQuantity.longValue() < entry.getValue().longValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 正式扣除材料。
     *
     * @param playerId 玩家ID
     * @param costMap 消耗 map
     */
    private void consumeCost(Long playerId, Map<Long, Long> costMap) {
        if (costMap == null || costMap.isEmpty()) {
            return;
        }

        for (Map.Entry<Long, Long> entry : costMap.entrySet()) {
            deductOneItem(playerId, entry.getKey(), entry.getValue());
        }
    }

    /**
     * 发放正式产物。
     *
     * @param playerId 玩家ID
     * @param serverId 区服ID
     * @param outputMap 产出 map
     */
    private void grantOutput(Long playerId, Integer serverId, Map<Long, Long> outputMap) {
        if (outputMap == null || outputMap.isEmpty()) {
            return;
        }

        for (Map.Entry<Long, Long> entry : outputMap.entrySet()) {
            Long itemId = entry.getKey();
            Long quantity = entry.getValue();
            if (itemId == null || quantity == null || quantity.longValue() <= 0L) {
                continue;
            }
            ItemDefEntity itemDef = itemDefMapper.selectById(itemId);
            if (isEquipmentItem(itemDef)) {
                grantEquipOutput(playerId, serverId, itemDef, quantity.longValue());
                continue;
            }
            addOneItem(playerId, serverId, itemId, quantity);
        }
    }

    /**
     * 汇总玩家正式库存数量。
     *
     * @param playerId 玩家ID
     * @param itemId 物品ID
     * @return 正式库存数量
     */
    private Long sumFormalQuantity(Long playerId, Long itemId) {
        ItemDefEntity itemDef = itemDefMapper.selectById(itemId);
        if (isEquipmentItem(itemDef)) {
            return sumEquipQuantity(playerId, itemId);
        }

        List<PlayerItemStackEntity> stackList = playerItemStackMapper.selectList(
                new LambdaQueryWrapper<PlayerItemStackEntity>()
                        .eq(PlayerItemStackEntity::getPlayerId, playerId)
                        .eq(PlayerItemStackEntity::getItemId, itemId)
                        .eq(PlayerItemStackEntity::getDeleteFlag, 0)
                        .orderByAsc(PlayerItemStackEntity::getId)
        );

        long total = 0L;
        if (stackList == null || stackList.isEmpty()) {
            return total;
        }

        for (int i = 0; i < stackList.size(); i++) {
            PlayerItemStackEntity stack = stackList.get(i);
            if (stack == null || stack.getQty() == null || stack.getQty().longValue() <= 0L) {
                continue;
            }
            total += stack.getQty().longValue();
        }
        return total;
    }

    /**
     * 汇总玩家装备实例数量。
     *
     * @param playerId 玩家ID
     * @param itemId 物品ID
     * @return 装备实例数量
     */
    private Long sumEquipQuantity(Long playerId, Long itemId) {
        List<PlayerEquipEntity> equipList = playerEquipMapper.selectList(
                new LambdaQueryWrapper<PlayerEquipEntity>()
                        .eq(PlayerEquipEntity::getPlayerId, playerId)
                        .eq(PlayerEquipEntity::getItemId, itemId)
                        .eq(PlayerEquipEntity::getState, 1)
                        .eq(PlayerEquipEntity::getDeleteFlag, 0)
                        .orderByAsc(PlayerEquipEntity::getId)
        );
        if (equipList == null || equipList.isEmpty()) {
            return 0L;
        }
        return (long) equipList.size();
    }

    /**
     * 扣除单种物品。
     *
     * @param playerId 玩家ID
     * @param itemId 物品ID
     * @param quantity 扣除数量
     */
    private void deductOneItem(Long playerId, Long itemId, Long quantity) {
        long remain = quantity == null ? 0L : quantity.longValue();
        if (remain <= 0L) {
            return;
        }

        ItemDefEntity itemDef = itemDefMapper.selectById(itemId);
        if (isEquipmentItem(itemDef)) {
            deductEquipItem(playerId, itemId, remain);
            return;
        }

        List<PlayerItemStackEntity> stackList = playerItemStackMapper.selectList(
                new LambdaQueryWrapper<PlayerItemStackEntity>()
                        .eq(PlayerItemStackEntity::getPlayerId, playerId)
                        .eq(PlayerItemStackEntity::getItemId, itemId)
                        .eq(PlayerItemStackEntity::getDeleteFlag, 0)
                        .orderByAsc(PlayerItemStackEntity::getId)
        );

        if (stackList == null || stackList.isEmpty()) {
            throw new IllegalStateException("材料不足，itemId=" + itemId);
        }

        for (int i = 0; i < stackList.size(); i++) {
            PlayerItemStackEntity stack = stackList.get(i);
            long currentQty = stack.getQty() == null ? 0L : stack.getQty().longValue();
            if (currentQty <= 0L) {
                continue;
            }

            if (currentQty >= remain) {
                stack.setQty(currentQty - remain);
                stack.setUpdateUser("-1");
                playerItemStackMapper.updateById(stack);
                return;
            }

            stack.setQty(0L);
            stack.setUpdateUser("-1");
            playerItemStackMapper.updateById(stack);
            remain -= currentQty;
        }

        if (remain > 0L) {
            throw new IllegalStateException("材料不足，itemId=" + itemId);
        }
    }

    /**
     * 扣除装备实例。
     *
     * @param playerId 玩家ID
     * @param itemId 装备物品ID
     * @param quantity 扣除数量
     */
    private void deductEquipItem(Long playerId, Long itemId, long quantity) {
        List<PlayerEquipEntity> equipList = playerEquipMapper.selectList(
                new LambdaQueryWrapper<PlayerEquipEntity>()
                        .eq(PlayerEquipEntity::getPlayerId, playerId)
                        .eq(PlayerEquipEntity::getItemId, itemId)
                        .eq(PlayerEquipEntity::getState, 1)
                        .eq(PlayerEquipEntity::getDeleteFlag, 0)
                        .orderByAsc(PlayerEquipEntity::getId)
        );
        if (equipList == null || equipList.size() < quantity) {
            throw new IllegalStateException("材料不足，itemId=" + itemId);
        }
        for (int i = 0; i < quantity; i++) {
            PlayerEquipEntity equip = equipList.get(i);
            equip.setState(0);
            equip.setDeleteFlag(1);
            equip.setUpdateUser("-1");
            playerEquipMapper.updateById(equip);
        }
    }

    /**
     * 增加单种物品。
     *
     * @param playerId 玩家ID
     * @param serverId 区服ID
     * @param itemId 物品ID
     * @param quantity 增加数量
     */
    private void addOneItem(Long playerId, Integer serverId, Long itemId, Long quantity) {
        if (quantity == null || quantity.longValue() <= 0L) {
            return;
        }

        PlayerItemStackEntity stack = playerItemStackMapper.selectOne(
                new LambdaQueryWrapper<PlayerItemStackEntity>()
                        .eq(PlayerItemStackEntity::getPlayerId, playerId)
                        .eq(PlayerItemStackEntity::getItemId, itemId)
                        .eq(PlayerItemStackEntity::getDeleteFlag, 0)
                        .orderByAsc(PlayerItemStackEntity::getId)
                        .last("limit 1")
        );

        if (stack == null) {
            insertNewStack(playerId, serverId, itemId, quantity);
            return;
        }

        long currentQty = stack.getQty() == null ? 0L : stack.getQty().longValue();
        stack.setQty(currentQty + quantity.longValue());
        stack.setUpdateUser("-1");
        int rows = playerItemStackMapper.updateById(stack);
        if (rows != 1) {
            throw new IllegalStateException("制造入库失败，itemId=" + itemId);
        }
    }

    /**
     * 插入新库存堆叠。
     *
     * @param playerId 玩家ID
     * @param serverId 区服ID
     * @param itemId 物品ID
     * @param quantity 数量
     */
    private void insertNewStack(Long playerId, Integer serverId, Long itemId, Long quantity) {
        ItemDefEntity itemDef = itemDefMapper.selectById(itemId);
        if (itemDef == null) {
            throw new IllegalStateException("物品定义不存在，itemId=" + itemId);
        }

        PlayerItemStackEntity stack = new PlayerItemStackEntity();
        stack.setPlayerId(playerId);
        stack.setServerId(serverId == null ? 1 : serverId);
        stack.setItemId(itemId);
        stack.setQty(quantity);
        stack.setLocation(1);
        stack.setRemarks("craft_reward");
        stack.setCreateUser("-1");
        stack.setUpdateUser("-1");

        int rows = playerItemStackMapper.insert(stack);
        if (rows != 1) {
            throw new IllegalStateException("新增制造产物失败，itemId=" + itemId);
        }
    }

    /**
     * 把根任务标记为材料不足停止。
     *
     * @param taskId 根任务ID
     * @param stopTime 停止时刻
     */
    private void stopByMaterialShortage(Long taskId, LocalDateTime stopTime) {
        PlayerActionTaskEntity entity = playerActionTaskMapper.selectById(taskId);
        if (entity == null) {
            return;
        }

        entity.setStatus(ActionTaskStatusEnum.STOPPED.getCode());
        entity.setStopReason(ActionTaskStopReasonEnum.MATERIAL_SHORTAGE.getCode());
        entity.setState(3);
        entity.setLastSettleTime(stopTime);
        entity.setLastCalcTime(stopTime);
        entity.setUpdateUser("-1");
        playerActionTaskMapper.updateById(entity);
    }

    /**
     * 把根任务标记为正常完成。
     *
     * @param taskId 根任务ID
     * @param finishTime 完成时刻
     */
    private void finishNormally(Long taskId, LocalDateTime finishTime) {
        PlayerActionTaskEntity entity = playerActionTaskMapper.selectById(taskId);
        if (entity == null) {
            return;
        }

        entity.setStatus(ActionTaskStatusEnum.COMPLETED.getCode());
        entity.setStopReason(ActionTaskStopReasonEnum.FINISHED.getCode());
        entity.setState(3);
        entity.setLastSettleTime(finishTime);
        entity.setLastCalcTime(finishTime);
        entity.setUpdateUser("-1");
        playerActionTaskMapper.updateById(entity);
    }

    /**
     * 更新根任务最近结算时间。
     *
     * @param taskId 根任务ID
     * @param settleTime 结算时间
     */
    private void updateRootSettleTime(Long taskId, LocalDateTime settleTime) {
        PlayerActionTaskEntity entity = playerActionTaskMapper.selectById(taskId);
        if (entity == null) {
            return;
        }

        entity.setLastSettleTime(settleTime);
        entity.setLastCalcTime(settleTime);
        entity.setUpdateUser("-1");
        playerActionTaskMapper.updateById(entity);
    }

    /**
     * 配方快照。
     */
    @Data
    private static class RecipeSnapshot {

        /**
         * 配方编码。
         */
        private String recipeCode;

        /**
         * 技能ID。
         */
        private Long skillId;

        /**
         * 技能编码。
         */
        private String skillCode;

        /**
         * 耗时。
         */
        private Integer craftTimeMs;

        /**
         * 需求等级。
         */
        private Integer craftLevelReq;

        /**
         * 经验。
         */
        private Long expGain;

        /**
         * 消耗 map。
         */
        private Map<Long, Long> costMap;

        /**
         * 产出 map。
         */
        private Map<Long, Long> outputMap;

        /**
         * 扩展 JSON。
         */
        private String metaJson;
    }
}