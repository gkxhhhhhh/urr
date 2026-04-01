package com.urr.app.action.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urr.app.action.task.CraftTaskAppService;
import com.urr.app.action.task.CraftTaskSettleService;
import com.urr.app.action.task.GatherTaskReadService;
import com.urr.app.action.task.PlayerActionQueueRepository;
import com.urr.app.action.task.PlayerActionTaskRepository;
import com.urr.app.action.task.PlayerCraftTaskRepository;
import com.urr.app.action.task.ProfessionActionReadService;
import com.urr.app.action.task.command.StartGatherTaskCommand;
import com.urr.app.action.task.query.QueryGatherTaskPanelQuery;
import com.urr.app.action.task.result.QueryGatherTaskPanelResult;
import com.urr.app.action.task.result.StartGatherTaskResult;
import com.urr.domain.action.ActionDefEntity;
import com.urr.domain.action.task.ActionTaskConstants;
import com.urr.domain.action.task.ActionTaskTypeEnum;
import com.urr.domain.action.task.PlayerActionQueueEntity;
import com.urr.domain.action.task.PlayerActionTask;
import com.urr.domain.action.task.PlayerCraftTask;
import com.urr.domain.item.ItemDefEntity;
import com.urr.domain.item.PlayerEquipEntity;
import com.urr.domain.item.PlayerItemStackEntity;
import com.urr.domain.player.PlayerEntity;
import com.urr.domain.skill.PlayerSkillEntity;
import com.urr.infra.mapper.ActionDefMapper;
import com.urr.infra.mapper.ItemDefMapper;
import com.urr.infra.mapper.PlayerEquipMapper;
import com.urr.infra.mapper.PlayerItemStackMapper;
import com.urr.infra.mapper.PlayerMapper;
import com.urr.infra.mapper.PlayerSkillMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用职业动作读服务实现。
 */
@Service
@RequiredArgsConstructor
public class ProfessionActionReadServiceImpl implements ProfessionActionReadService {

    /**
     * 玩家 Mapper。
     */
    private final PlayerMapper playerMapper;

    /**
     * 根任务仓储。
     */
    private final PlayerActionTaskRepository playerActionTaskRepository;

    /**
     * 队列仓储。
     */
    private final PlayerActionQueueRepository playerActionQueueRepository;

    /**
     * 制造任务仓储。
     */
    private final PlayerCraftTaskRepository playerCraftTaskRepository;

    /**
     * 制造结算服务。
     */
    private final CraftTaskSettleService craftTaskSettleService;

    /**
     * 制造任务应用服务。
     */
    private final CraftTaskAppService craftTaskAppService;

    /**
     * 采集读服务。
     */
    private final GatherTaskReadService gatherTaskReadService;

    /**
     * 动作定义 Mapper。
     */
    private final ActionDefMapper actionDefMapper;

    /**
     * 物品定义 Mapper。
     */
    private final ItemDefMapper itemDefMapper;

    /**
     * 玩家背包 Mapper。
     */
    private final PlayerItemStackMapper playerItemStackMapper;

    /**
     * 玩家装备实例 Mapper。
     */
    private final PlayerEquipMapper playerEquipMapper;

    /**
     * 玩家技能 Mapper。
     */
    private final PlayerSkillMapper playerSkillMapper;

    /**
     * JSON 工具。
     */
    private final ObjectMapper objectMapper;

    /**
     * 查询职业动作面板。
     *
     * @param query 查询参数
     * @return 面板结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public QueryGatherTaskPanelResult queryPanel(QueryGatherTaskPanelQuery query) {
        validateQuery(query);
        PlayerEntity player = requireOwnedPlayer(query.getAccountId(), query.getPlayerId());
        LocalDateTime readTime = query.getReadTime() == null ? LocalDateTime.now() : query.getReadTime();

        PlayerActionTask runningTask = loadLatestRunningTask(player.getId(), readTime);
        if (runningTask == null) {
            runningTask = tryAutoStartFirstCraftQueue(query.getAccountId(), player.getId());
        }

        if (runningTask != null && ActionTaskTypeEnum.GATHER.equals(runningTask.getTaskType())) {
            return gatherTaskReadService.queryPanel(query);
        }

        QueryGatherTaskPanelResult result = QueryGatherTaskPanelResult.createEmpty(player.getId(), readTime);

        if (runningTask != null && ActionTaskTypeEnum.CRAFT.equals(runningTask.getTaskType())) {
            PlayerCraftTask craftTask = playerCraftTaskRepository.findByTaskId(runningTask.getId());
            if (craftTask != null && craftTask.isRunning()) {
                result.setCurrentTask(buildRuntimeView(craftTask));
                result.setHasRunningTask(Boolean.TRUE);
            }
        }

        List<PlayerActionQueueEntity> queueEntityList =
                playerActionQueueRepository.findQueuedByPlayerIdAndTaskType(player.getId(), ActionTaskTypeEnum.CRAFT);
        result.setQueueList(buildQueueViewList(queueEntityList));
        result.setQueueSize(queueEntityList == null ? 0 : queueEntityList.size());
        result.setPendingRewardPool(QueryGatherTaskPanelResult.PendingRewardPoolView.createEmpty());
        result.setDisplayInventory(buildInventoryView(player.getId()));
        return result;
    }

    /**
     * 加载当前最新运行任务。
     *
     * @param playerId 角色ID
     * @param readTime 读取时间
     * @return 运行任务
     */
    private PlayerActionTask loadLatestRunningTask(Long playerId, LocalDateTime readTime) {
        PlayerActionTask runningTask = playerActionTaskRepository.findRunningByPlayerId(playerId);
        if (runningTask == null) {
            return null;
        }
        if (ActionTaskTypeEnum.GATHER.equals(runningTask.getTaskType())) {
            return runningTask;
        }
        if (!ActionTaskTypeEnum.CRAFT.equals(runningTask.getTaskType())) {
            return runningTask;
        }

        craftTaskSettleService.settleTo(runningTask.getId(), readTime);
        return playerActionTaskRepository.findRunningByPlayerId(playerId);
    }

    /**
     * 在当前没有运行任务时，尝试自动拉起第一条制造队列。
     *
     * @param accountId 账号ID
     * @param playerId 角色ID
     * @return 最新运行任务
     */
    private PlayerActionTask tryAutoStartFirstCraftQueue(Long accountId, Long playerId) {
        PlayerActionQueueEntity firstQueue =
                playerActionQueueRepository.findFirstQueuedByPlayerIdAndTaskType(playerId, ActionTaskTypeEnum.CRAFT);
        if (firstQueue == null) {
            return null;
        }

        StartGatherTaskCommand startCommand = new StartGatherTaskCommand();
        startCommand.setAccountId(accountId);
        startCommand.setPlayerId(playerId);
        startCommand.setActionCode(firstQueue.getActionCode());
        startCommand.setTargetCount(firstQueue.getTargetCount());

        StartGatherTaskResult startResult = craftTaskAppService.startNow(startCommand);
        if (startResult == null || startResult.getTaskId() == null) {
            return null;
        }

        int deleteRows = playerActionQueueRepository.deleteQueuedById(playerId, firstQueue.getId());
        if (deleteRows != 1) {
            throw new IllegalStateException("制造队列自动启动后删除队列失败，queueId=" + firstQueue.getId());
        }

        return playerActionTaskRepository.findRunningByPlayerId(playerId);
    }

    /**
     * 校验查询参数。
     *
     * @param query 查询参数
     */
    private void validateQuery(QueryGatherTaskPanelQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("查询参数不能为空");
        }
        if (query.getAccountId() == null) {
            throw new IllegalArgumentException("accountId不能为空");
        }
        if (query.getPlayerId() == null) {
            throw new IllegalArgumentException("playerId不能为空");
        }
    }

    /**
     * 校验玩家归属。
     *
     * @param accountId 账号ID
     * @param playerId 角色ID
     * @return 玩家
     */
    private PlayerEntity requireOwnedPlayer(Long accountId, Long playerId) {
        PlayerEntity player = playerMapper.selectOne(
                new LambdaQueryWrapper<PlayerEntity>()
                        .eq(PlayerEntity::getId, playerId)
                        .eq(PlayerEntity::getAccountId, accountId)
                        .eq(PlayerEntity::getDeleteFlag, 0)
                        .last("limit 1")
        );
        if (player == null) {
            throw new IllegalStateException("角色不存在或无权限操作");
        }
        return player;
    }

    /**
     * 构建运行中任务视图。
     *
     * @param craftTask 制造任务
     * @return 运行视图
     */
    private QueryGatherTaskPanelResult.RuntimeView buildRuntimeView(PlayerCraftTask craftTask) {
        if (craftTask == null) {
            return null;
        }

        CraftSnapshot snapshot = parseCraftSnapshot(craftTask.getRecipeSnapshotJson());

        QueryGatherTaskPanelResult.RuntimeView view = new QueryGatherTaskPanelResult.RuntimeView();
        view.setTaskId(craftTask.getId());
        view.setActionCode(craftTask.getActionCode());
        view.setActionName(resolveActionName(craftTask.getActionCode()));
        view.setStatus(craftTask.getStatus() == null ? null : craftTask.getStatus().getCode());
        view.setTargetCount(craftTask.isInfiniteTarget() ? -1L : craftTask.getTargetCount());
        view.setInfiniteTarget(craftTask.isInfiniteTarget());
        view.setCompletedCount(craftTask.getCompletedCount() == null ? 0L : craftTask.getCompletedCount());
        view.setFlushedCount(craftTask.getCompletedCount() == null ? 0L : craftTask.getCompletedCount());
        view.setRemainingCount(
                craftTask.isInfiniteTarget()
                        ? null
                        : Math.max(craftTask.getTargetCount() - craftTask.getSafeCompletedCount(), 0L)
        );
        view.setCurrentSegmentStart(craftTask.getSafeCompletedCount() + 1L);
        view.setCurrentSegmentEnd(craftTask.getSafeCompletedCount() + 1L);
        view.setSegmentSize(1);
        view.setStartTime(craftTask.getStartTime());
        view.setLastSettleTime(craftTask.getLastSettleTime());
        view.setOfflineExpireAt(craftTask.getOfflineExpireAt());

        QueryGatherTaskPanelResult.SnapshotView snapshotView = new QueryGatherTaskPanelResult.SnapshotView();
        snapshotView.setGatherLevel(resolveSkillLevel(craftTask.getPlayerId(), snapshot.getSkillId()));
        snapshotView.setGatherDurationMs(snapshot.getCraftTimeMs());
        snapshotView.setGatherEfficiency(null);
        snapshotView.setOfflineMinutesLimit(ActionTaskConstants.DEFAULT_OFFLINE_HOURS * 60);
        view.setSnapshot(snapshotView);

        QueryGatherTaskPanelResult.SegmentView segmentView = new QueryGatherTaskPanelResult.SegmentView();
        segmentView.setSegmentStart(craftTask.getSafeCompletedCount() + 1L);
        segmentView.setSegmentEnd(craftTask.getSafeCompletedCount() + 1L);
        segmentView.setSegmentSize(1);
        segmentView.setRewardSeed(craftTask.getRewardSeed());
        segmentView.setLockedRoundCount(1);
        segmentView.setCompletedCountInSegment(0L);
        segmentView.setRemainingCountInSegment(1L);
        view.setCurrentSegment(segmentView);

        view.setPendingRewardPool(QueryGatherTaskPanelResult.PendingRewardPoolView.createEmpty());
        return view;
    }

    /**
     * 构建队列视图。
     *
     * @param queueEntityList 队列实体列表
     * @return 队列视图
     */
    private List<QueryGatherTaskPanelResult.QueueItemView> buildQueueViewList(
            List<PlayerActionQueueEntity> queueEntityList
    ) {
        List<QueryGatherTaskPanelResult.QueueItemView> result = new ArrayList<>();
        if (queueEntityList == null || queueEntityList.isEmpty()) {
            return result;
        }

        for (int i = 0; i < queueEntityList.size(); i++) {
            PlayerActionQueueEntity queueEntity = queueEntityList.get(i);
            if (queueEntity == null) {
                continue;
            }

            QueryGatherTaskPanelResult.QueueItemView view = new QueryGatherTaskPanelResult.QueueItemView();
            view.setQueueId(queueEntity.getId());
            view.setTaskId(null);
            view.setActionCode(queueEntity.getActionCode());
            view.setActionName(resolveActionName(queueEntity.getActionCode()));
            view.setStatus(queueEntity.getStatus());
            view.setQueuePosition(i + 1);
            view.setTargetCount(queueEntity.getTargetCount() == null ? 0L : queueEntity.getTargetCount());
            view.setInfiniteTarget(
                    queueEntity.getTargetCount() != null
                            && queueEntity.getTargetCount().longValue() == ActionTaskConstants.INFINITE_TARGET_COUNT
            );
            result.add(view);
        }
        return result;
    }

    /**
     * 构建展示库存。
     *
     * @param playerId 玩家ID
     * @return 库存视图
     */
    private QueryGatherTaskPanelResult.InventoryView buildInventoryView(Long playerId) {
        QueryGatherTaskPanelResult.InventoryView view = QueryGatherTaskPanelResult.InventoryView.createEmpty();
        List<QueryGatherTaskPanelResult.InventoryEntryView> entryList = new ArrayList<>();

        List<PlayerEquipEntity> equipList = playerEquipMapper.selectList(
                new LambdaQueryWrapper<PlayerEquipEntity>()
                        .eq(PlayerEquipEntity::getPlayerId, playerId)
                        .eq(PlayerEquipEntity::getState, 1)
                        .eq(PlayerEquipEntity::getDeleteFlag, 0)
                        .orderByDesc(PlayerEquipEntity::getId)
        );
        if (equipList != null && !equipList.isEmpty()) {
            for (int i = 0; i < equipList.size(); i++) {
                PlayerEquipEntity equip = equipList.get(i);
                if (equip == null) {
                    continue;
                }
                QueryGatherTaskPanelResult.InventoryEntryView equipEntry = buildEquipInventoryEntry(equip);
                if (equipEntry != null) {
                    entryList.add(equipEntry);
                }
            }
        }

        List<PlayerItemStackEntity> stackList = playerItemStackMapper.selectList(
                new LambdaQueryWrapper<PlayerItemStackEntity>()
                        .eq(PlayerItemStackEntity::getPlayerId, playerId)
                        .eq(PlayerItemStackEntity::getDeleteFlag, 0)
                        .orderByDesc(PlayerItemStackEntity::getQty)
                        .orderByAsc(PlayerItemStackEntity::getId)
        );
        if (stackList != null && !stackList.isEmpty()) {
            for (int i = 0; i < stackList.size(); i++) {
                PlayerItemStackEntity stack = stackList.get(i);
                if (stack == null || stack.getQty() == null || stack.getQty().longValue() <= 0L) {
                    continue;
                }
                QueryGatherTaskPanelResult.InventoryEntryView stackEntry = buildStackInventoryEntry(stack);
                if (stackEntry != null) {
                    entryList.add(stackEntry);
                }
            }
        }

        view.setEntryList(entryList);
        view.setEntryCount(entryList.size());
        return view;
    }

    /**
     * 构建堆叠物品展示项。
     *
     * @param stack 堆叠物品
     * @return 展示项
     */
    private QueryGatherTaskPanelResult.InventoryEntryView buildStackInventoryEntry(PlayerItemStackEntity stack) {
        if (stack == null) {
            return null;
        }
        ItemDefEntity itemDef = itemDefMapper.selectById(stack.getItemId());
        QueryGatherTaskPanelResult.InventoryEntryView entryView = new QueryGatherTaskPanelResult.InventoryEntryView();
        entryView.setRewardType("ITEM");
        entryView.setRewardCode(itemDef == null ? String.valueOf(stack.getItemId()) : itemDef.getItemCode());
        entryView.setRewardName(itemDef == null ? String.valueOf(stack.getItemId()) : itemDef.getNameZh());
        entryView.setFormalQuantity(stack.getQty());
        entryView.setPendingQuantity(0L);
        entryView.setDisplayQuantity(stack.getQty());
        entryView.setItemId(stack.getItemId());
        entryView.setItemType(itemDef == null ? null : itemDef.getItemType());
        entryView.setItemLevel(resolveItemLevel(itemDef, null));
        entryView.setEquipCategory(resolveEquipCategory(itemDef));
        return entryView;
    }

    /**
     * 构建设备实例展示项。
     *
     * @param equip 装备实例
     * @return 展示项
     */
    private QueryGatherTaskPanelResult.InventoryEntryView buildEquipInventoryEntry(PlayerEquipEntity equip) {
        if (equip == null || equip.getItemId() == null) {
            return null;
        }
        ItemDefEntity itemDef = itemDefMapper.selectById(equip.getItemId());
        JsonNode itemMetaNode = parseJsonNode(itemDef == null ? null : itemDef.getMetaJson());
        JsonNode attrNode = parseJsonNode(equip.getAttrJson());

        QueryGatherTaskPanelResult.InventoryEntryView entryView = new QueryGatherTaskPanelResult.InventoryEntryView();
        entryView.setRewardType("EQUIP");
        entryView.setRewardCode(itemDef == null ? String.valueOf(equip.getItemId()) : itemDef.getItemCode());
        entryView.setRewardName(itemDef == null ? String.valueOf(equip.getItemId()) : itemDef.getNameZh());
        entryView.setFormalQuantity(1L);
        entryView.setPendingQuantity(0L);
        entryView.setDisplayQuantity(1L);
        entryView.setItemId(equip.getItemId());
        entryView.setEquipInstanceId(equip.getId());
        entryView.setItemType(itemDef == null ? 4 : itemDef.getItemType());
        entryView.setItemLevel(resolveItemLevel(itemDef, attrNode));
        entryView.setStrengthenLevel(readInt(attrNode, "strengthenLevel", 0));
        entryView.setEquipCategory(resolveEquipCategory(itemDef));
        entryView.setBaseAttack(readDouble(attrNode, "baseAttack", readDouble(itemMetaNode.path("baseAttrs"), "attack", 0D)));
        entryView.setCurrentAttack(readDouble(attrNode, "currentAttack", entryView.getBaseAttack() == null ? 0D : entryView.getBaseAttack()));
        return entryView;
    }

    /**
     * 解析装备类别。
     *
     * @param itemDef 物品定义
     * @return 装备类别
     */
    private String resolveEquipCategory(ItemDefEntity itemDef) {
        JsonNode itemMetaNode = parseJsonNode(itemDef == null ? null : itemDef.getMetaJson());
        return readText(itemMetaNode, "equipCategory");
    }

    /**
     * 解析物品等级。
     *
     * @param itemDef 物品定义
     * @param attrNode 装备扩展属性
     * @return 物品等级
     */
    private Integer resolveItemLevel(ItemDefEntity itemDef, JsonNode attrNode) {
        int attrItemLevel = readInt(attrNode, "itemLevel", 0);
        if (attrItemLevel > 0) {
            return attrItemLevel;
        }
        JsonNode itemMetaNode = parseJsonNode(itemDef == null ? null : itemDef.getMetaJson());
        int metaItemLevel = readInt(itemMetaNode, "itemLevel", 0);
        if (metaItemLevel > 0) {
            return metaItemLevel;
        }
        int tier = readInt(itemMetaNode, "tier", 1);
        return tier <= 0 ? 1 : tier;
    }

    /**
     * 解析 JSON 节点。
     *
     * @param json JSON 文本
     * @return JSON 节点
     */
    private JsonNode parseJsonNode(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ignore) {
            return null;
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
        if (root == null || !StringUtils.hasText(fieldName)) {
            return null;
        }
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 读取整数值。
     *
     * @param root 根节点
     * @param fieldName 字段名
     * @param defaultValue 默认值
     * @return 整数值
     */
    private Integer readInt(JsonNode root, String fieldName, int defaultValue) {
        if (root == null || !StringUtils.hasText(fieldName)) {
            return defaultValue;
        }
        JsonNode node = root.get(fieldName);
        return node == null || node.isNull() ? defaultValue : node.asInt(defaultValue);
    }

    /**
     * 读取小数值。
     *
     * @param root 根节点
     * @param fieldName 字段名
     * @param defaultValue 默认值
     * @return 小数值
     */
    private Double readDouble(JsonNode root, String fieldName, double defaultValue) {
        if (root == null || !StringUtils.hasText(fieldName)) {
            return defaultValue;
        }
        JsonNode node = root.get(fieldName);
        return node == null || node.isNull() ? defaultValue : node.asDouble(defaultValue);
    }

    /**
     * 解析动作名称。
     *
     * @param actionCode 动作编码
     * @return 动作名
     */
    private String resolveActionName(String actionCode) {
        if (!StringUtils.hasText(actionCode)) {
            return actionCode;
        }

        ActionDefEntity actionDef = actionDefMapper.selectOne(
                new LambdaQueryWrapper<ActionDefEntity>()
                        .eq(ActionDefEntity::getActionCode, actionCode.trim())
                        .eq(ActionDefEntity::getDeleteFlag, 0)
                        .last("limit 1")
        );
        return actionDef == null ? actionCode : actionDef.getActionName();
    }

    /**
     * 解析玩家技能等级。
     *
     * @param playerId 玩家ID
     * @param skillId 技能ID
     * @return 技能等级
     */
    private Integer resolveSkillLevel(Long playerId, Long skillId) {
        if (playerId == null || skillId == null) {
            return 1;
        }

        PlayerSkillEntity playerSkill = playerSkillMapper.selectOne(
                new LambdaQueryWrapper<PlayerSkillEntity>()
                        .eq(PlayerSkillEntity::getPlayerId, playerId)
                        .eq(PlayerSkillEntity::getSkillId, skillId)
                        .eq(PlayerSkillEntity::getDeleteFlag, 0)
                        .last("limit 1")
        );
        if (playerSkill == null || playerSkill.getSkillLevel() == null || playerSkill.getSkillLevel() <= 0) {
            return 1;
        }
        return playerSkill.getSkillLevel();
    }

    /**
     * 解析制造快照。
     *
     * @param snapshotJson 快照 JSON
     * @return 快照
     */
    private CraftSnapshot parseCraftSnapshot(String snapshotJson) {
        CraftSnapshot snapshot = new CraftSnapshot();
        if (!StringUtils.hasText(snapshotJson)) {
            snapshot.setCraftTimeMs(0);
            return snapshot;
        }

        try {
            JsonNode root = objectMapper.readTree(snapshotJson);
            snapshot.setSkillId(readLong(root, "skillId", null));
            snapshot.setCraftTimeMs(readInt(root, "craftTimeMs", 0));
            snapshot.setCostMap(readLongMap(root.get("costMap")));
            snapshot.setOutputMap(readLongMap(root.get("outputMap")));
            return snapshot;
        } catch (Exception exception) {
            throw new IllegalStateException("解析制造快照失败", exception);
        }
    }

    /**
     * 读取 long 字段。
     *
     * @param root 根节点
     * @param fieldName 字段名
     * @param defaultValue 默认值
     * @return long 值
     */
    private Long readLong(JsonNode root, String fieldName, Long defaultValue) {
        if (root == null || fieldName == null) {
            return defaultValue;
        }
        JsonNode node = root.get(fieldName);
        return node == null || node.isNull() ? defaultValue : node.asLong();
    }

    /**
     * 读取 int 字段。
     *
     * @param root 根节点
     * @param fieldName 字段名
     * @param defaultValue 默认值
     * @return int 值
     */
    private Integer readInt(JsonNode root, String fieldName, Integer defaultValue) {
        if (root == null || fieldName == null) {
            return defaultValue;
        }
        JsonNode node = root.get(fieldName);
        return node == null || node.isNull() ? defaultValue : node.asInt();
    }

    /**
     * 把 JSON 对象转成 long map。
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
            result.put(Long.parseLong(entry.getKey()), entry.getValue().asLong(0L));
        }
        return result;
    }

    /**
     * 制造快照。
     */
    @Data
    private static class CraftSnapshot {

        /**
         * 技能ID。
         */
        private Long skillId;

        /**
         * 制造耗时。
         */
        private Integer craftTimeMs;

        /**
         * 消耗 map。
         */
        private Map<Long, Long> costMap;

        /**
         * 产出 map。
         */
        private Map<Long, Long> outputMap;
    }
}