package com.urr.app.action.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urr.app.action.task.GatherTaskAdvanceService;
import com.urr.app.action.task.GatherTaskReadService;
import com.urr.app.action.task.GatherTaskViewAssembler;
import com.urr.app.action.task.PlayerActionQueueRepository;
import com.urr.app.action.task.PlayerActionTaskRepository;
import com.urr.app.action.task.PlayerGatherTaskRedisRepository;
import com.urr.app.action.task.PlayerGatherTaskRepository;
import com.urr.app.action.task.command.AdvanceGatherTaskCommand;
import com.urr.app.action.task.query.QueryGatherTaskPanelQuery;
import com.urr.app.action.task.result.QueryGatherTaskPanelResult;
import com.urr.domain.action.ActionDefEntity;
import com.urr.domain.action.task.ActionTaskTypeEnum;
import com.urr.domain.action.task.GatherTaskPendingRewardPoolCache;
import com.urr.domain.action.task.GatherTaskSegmentPlanCache;
import com.urr.domain.action.task.PlayerActionQueueEntity;
import com.urr.domain.action.task.PlayerActionTask;
import com.urr.domain.action.task.PlayerGatherTask;
import com.urr.domain.action.task.PlayerGatherTaskRuntimeCache;
import com.urr.domain.item.ItemDefEntity;
import com.urr.domain.item.PlayerItemStackEntity;
import com.urr.domain.player.PlayerEntity;
import com.urr.domain.wallet.WalletEntity;
import com.urr.infra.mapper.ActionDefMapper;
import com.urr.infra.mapper.ItemDefMapper;
import com.urr.infra.mapper.PlayerItemStackMapper;
import com.urr.infra.mapper.PlayerMapper;
import com.urr.infra.mapper.WalletMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 采集任务读服务实现。
 *
 * 说明：
 * 1. 这里只补“采集面板”需要的最小读能力。
 * 2. 运行态 / segment / pending 优先读 Redis 热态，缺失时回退到数据库镜像字段。
 * 3. 正式库存直接读正式表，不额外发明新的库存系统。
 */
@Service
@RequiredArgsConstructor
public class GatherTaskReadServiceImpl implements GatherTaskReadService {

    /**
     * 角色 Mapper。
     */
    private final PlayerMapper playerMapper;

    /**
     * 动作根任务仓储。
     */
    private final PlayerActionTaskRepository playerActionTaskRepository;

    /**
     * 采集任务仓储。
     */
    private final PlayerGatherTaskRepository playerGatherTaskRepository;

    /**
     * 采集任务热态仓储。
     */
    private final PlayerGatherTaskRedisRepository playerGatherTaskRedisRepository;

    /**
     * 动作队列仓储。
     */
    private final PlayerActionQueueRepository playerActionQueueRepository;

    /**
     * 最小懒推进服务。
     */
    private final GatherTaskAdvanceService gatherTaskAdvanceService;

    /**
     * 动作定义 Mapper。
     */
    private final ActionDefMapper actionDefMapper;

    /**
     * 背包 Mapper。
     */
    private final PlayerItemStackMapper playerItemStackMapper;

    /**
     * 钱包 Mapper。
     */
    private final WalletMapper walletMapper;

    /**
     * 物品定义 Mapper。
     */
    private final ItemDefMapper itemDefMapper;

    /**
     * JSON 编解码器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 视图组装器。
     */
    private final GatherTaskViewAssembler gatherTaskViewAssembler;

    /**
     * 查询采集面板最小视图。
     *
     * @param query 查询参数
     * @return 面板结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public QueryGatherTaskPanelResult queryPanel(QueryGatherTaskPanelQuery query) {
        validateQuery(query);
        PlayerEntity player = getOwnedPlayer(query.getAccountId(), query.getPlayerId());
        LocalDateTime readTime = query.getReadTime() == null ? LocalDateTime.now() : query.getReadTime();

        PlayerGatherTask currentRunningTask = loadCurrentRunningGatherTask(player.getId(), readTime);
        PlayerGatherTask currentOrLatestGatherTask = currentRunningTask;
        if (currentOrLatestGatherTask == null) {
            currentOrLatestGatherTask = playerGatherTaskRepository.findCurrentByPlayerId(player.getId());
        }

        List<PlayerGatherTask> pendingTaskList = playerGatherTaskRepository.findPendingRewardTaskListByPlayerId(player.getId());
        PendingRewardData currentPendingData = loadCurrentPendingData(currentRunningTask, currentOrLatestGatherTask, pendingTaskList);
        Map<String, Long> pendingInventoryMap = buildPendingInventoryMap(pendingTaskList);

        List<PlayerActionQueueEntity> queueEntityList = playerActionQueueRepository.findQueuedByPlayerIdAndTaskType(player.getId(), ActionTaskTypeEnum.GATHER);
        Map<String, String> actionNameMap = loadActionNameMap(currentRunningTask, currentOrLatestGatherTask, queueEntityList, pendingTaskList);

        QueryGatherTaskPanelResult.PendingRewardPoolView pendingRewardPoolView = buildPendingRewardPoolView(currentPendingData);
        QueryGatherTaskPanelResult.RuntimeView runtimeView = buildRuntimeView(currentRunningTask, pendingRewardPoolView, actionNameMap);
        List<QueryGatherTaskPanelResult.QueueItemView> queueViewList = buildQueueViewList(queueEntityList, actionNameMap);
        QueryGatherTaskPanelResult.InventoryView inventoryView = buildInventoryView(player.getId(), pendingInventoryMap);

        return gatherTaskViewAssembler.buildPanelResult(player.getId(), readTime, runtimeView, queueViewList, pendingRewardPoolView, inventoryView);
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
     * 查询并校验角色归属。
     *
     * @param accountId 账号ID
     * @param playerId 角色ID
     * @return 角色实体
     */
    private PlayerEntity getOwnedPlayer(Long accountId, Long playerId) {
        PlayerEntity player = playerMapper.selectById(playerId);
        if (player == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        if (!accountId.equals(player.getAccountId())) {
            throw new IllegalArgumentException("无权限读取该角色");
        }
        return player;
    }

    /**
     * 读取当前运行中的采集任务。
     *
     * 说明：
     * 1. 只有当前运行任务确实是 GATHER 时，才会进入采集读链路。
     * 2. 读之前会做一次非常克制的最小 advance，确保进度 / pending 展示尽量接近当前时刻。
     *
     * @param playerId 角色ID
     * @param readTime 读取时刻
     * @return 当前运行中的采集任务，不存在时返回 null
     */
    private PlayerGatherTask loadCurrentRunningGatherTask(Long playerId, LocalDateTime readTime) {
        PlayerActionTask runningTask = playerActionTaskRepository.findRunningByPlayerId(playerId);
        if (runningTask == null) {
            return null;
        }
        if (!ActionTaskTypeEnum.GATHER.equals(runningTask.getTaskType())) {
            return null;
        }

        AdvanceGatherTaskCommand command = new AdvanceGatherTaskCommand();
        command.setTaskId(runningTask.getId());
        command.setAdvanceTime(readTime);
        gatherTaskAdvanceService.advanceTo(command);
        return playerGatherTaskRepository.findByTaskId(runningTask.getId());
    }

    /**
     * 读取当前 pending_reward_pool 数据。
     *
     * 说明：
     * 1. 优先取当前运行中的采集任务。
     * 2. 当前无运行任务时，回退到最近一条采集任务；如果最近一条没有 pending，再取最近一条有 pending 的采集任务。
     *
     * @param currentRunningTask 当前运行中的采集任务
     * @param currentOrLatestGatherTask 当前或最近采集任务
     * @param pendingTaskList 有 pending 的采集任务列表
     * @return 当前 pending 数据
     */
    private PendingRewardData loadCurrentPendingData(PlayerGatherTask currentRunningTask,
                                                     PlayerGatherTask currentOrLatestGatherTask,
                                                     List<PlayerGatherTask> pendingTaskList) {
        if (currentRunningTask != null) {
            PendingRewardData data = loadPendingRewardDataByTask(currentRunningTask);
            if (data.hasPending()) {
                return data;
            }
        }
        if (currentOrLatestGatherTask != null) {
            PendingRewardData data = loadPendingRewardDataByTask(currentOrLatestGatherTask);
            if (data.hasPending()) {
                return data;
            }
        }
        if (pendingTaskList != null && !pendingTaskList.isEmpty()) {
            return loadPendingRewardDataByTask(pendingTaskList.get(0));
        }
        return PendingRewardData.empty();
    }

    /**
     * 按任务读取 pending_reward_pool 数据。
     *
     * @param task 采集任务
     * @return pending 数据
     */
    private PendingRewardData loadPendingRewardDataByTask(PlayerGatherTask task) {
        if (task == null || task.getId() == null) {
            return PendingRewardData.empty();
        }

        GatherTaskPendingRewardPoolCache pendingCache = playerGatherTaskRedisRepository.findPendingRewardPoolByTaskId(task.getId());
        PendingRewardData data = new PendingRewardData();
        data.setTaskId(task.getId());
        data.setCompletedCount(task.getCompletedCount());
        data.setFlushedCount(task.getFlushedCount());
        data.setPendingRoundCount(task.getPendingRewardRoundCount());

        String rewardPoolJson = null;
        if (pendingCache != null) {
            if (pendingCache.getCompletedCount() != null) {
                data.setCompletedCount(pendingCache.getCompletedCount());
            }
            if (pendingCache.getFlushedCount() != null) {
                data.setFlushedCount(pendingCache.getFlushedCount());
            }
            if (pendingCache.getPendingRoundCount() != null) {
                data.setPendingRoundCount(pendingCache.getPendingRoundCount());
            }
            rewardPoolJson = pendingCache.getRewardPoolJson();
        }
        if (!StringUtils.hasText(rewardPoolJson)) {
            rewardPoolJson = task.getPendingRewardPoolJson();
        }
        data.setRewardList(parseRewardList(rewardPoolJson));
        return data;
    }

    /**
     * 组装运行任务视图。
     *
     * @param task 当前运行中的采集任务
     * @param pendingRewardPoolView 当前 pending 视图
     * @param actionNameMap 动作名称映射
     * @return 运行任务视图
     */
    private QueryGatherTaskPanelResult.RuntimeView buildRuntimeView(PlayerGatherTask task,
                                                                    QueryGatherTaskPanelResult.PendingRewardPoolView pendingRewardPoolView,
                                                                    Map<String, String> actionNameMap) {
        if (task == null) {
            return null;
        }

        PlayerGatherTaskRuntimeCache runtimeCache = playerGatherTaskRedisRepository.findRuntimeByTaskId(task.getId());
        GatherTaskSegmentPlanCache segmentCache = playerGatherTaskRedisRepository.findSegmentPlanByTaskId(task.getId());
        SegmentPlanSummary segmentPlanSummary = loadSegmentPlanSummary(task, segmentCache);

        if (runtimeCache != null) {
            copyRuntimeCacheToTask(task, runtimeCache);
        }

        QueryGatherTaskPanelResult.SegmentView segmentView = gatherTaskViewAssembler.buildSegmentView(
                segmentPlanSummary.getSegmentStart(),
                segmentPlanSummary.getSegmentEnd(),
                segmentPlanSummary.getSegmentSize(),
                segmentPlanSummary.getRewardSeed(),
                segmentPlanSummary.getLockedRoundCount(),
                task.getCompletedCount()
        );

        String actionName = actionNameMap.get(task.getActionCode());
        return gatherTaskViewAssembler.buildRuntimeView(task, actionName, pendingRewardPoolView, segmentView);
    }

    /**
     * 从热态缓存复制运行字段到任务对象。
     *
     * @param task 采集任务
     * @param runtimeCache 运行态缓存
     */
    private void copyRuntimeCacheToTask(PlayerGatherTask task, PlayerGatherTaskRuntimeCache runtimeCache) {
        if (task == null || runtimeCache == null) {
            return;
        }
        task.setCompletedCount(runtimeCache.getCompletedCount());
        task.setFlushedCount(runtimeCache.getFlushedCount());
        task.setCurrentSegmentStart(runtimeCache.getCurrentSegmentStart());
        task.setCurrentSegmentEnd(runtimeCache.getCurrentSegmentEnd());
        task.setSegmentSize(runtimeCache.getSegmentSize());
        task.setLastSettleTime(runtimeCache.getLastSettleTime());
        task.setOfflineExpireAt(runtimeCache.getOfflineExpireAt());
        if (runtimeCache.getStatSnapshot() != null) {
            task.setStatSnapshot(runtimeCache.getStatSnapshot());
        }
    }

    /**
     * 读取当前 segment 的最小摘要。
     *
     * @param task 采集任务
     * @param segmentCache segment 热态缓存
     * @return 分段摘要
     */
    private SegmentPlanSummary loadSegmentPlanSummary(PlayerGatherTask task, GatherTaskSegmentPlanCache segmentCache) {
        SegmentPlanSummary summary = new SegmentPlanSummary();
        summary.setSegmentStart(task.getCurrentSegmentStart());
        summary.setSegmentEnd(task.getCurrentSegmentEnd());
        summary.setSegmentSize(task.getSegmentSize());
        summary.setRewardSeed(task.getRewardSeed());
        summary.setLockedRoundCount(0);

        String planJson = null;
        if (segmentCache != null) {
            if (segmentCache.getSegmentStart() != null) {
                summary.setSegmentStart(segmentCache.getSegmentStart());
            }
            if (segmentCache.getSegmentEnd() != null) {
                summary.setSegmentEnd(segmentCache.getSegmentEnd());
            }
            if (segmentCache.getSegmentSize() != null) {
                summary.setSegmentSize(segmentCache.getSegmentSize());
            }
            if (segmentCache.getRewardSeed() != null) {
                summary.setRewardSeed(segmentCache.getRewardSeed());
            }
            planJson = segmentCache.getPlanJson();
        }
        if (!StringUtils.hasText(planJson)) {
            planJson = task.getCurrentSegmentRewardPlanJson();
        }
        fillSegmentPlanFromJson(summary, planJson);
        return summary;
    }

    /**
     * 从 JSON 中补充分段摘要。
     *
     * @param summary 分段摘要
     * @param planJson 分段计划 JSON
     */
    private void fillSegmentPlanFromJson(SegmentPlanSummary summary, String planJson) {
        if (summary == null || !StringUtils.hasText(planJson)) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(planJson);
            if (root.hasNonNull("segmentStart")) {
                summary.setSegmentStart(root.get("segmentStart").asLong());
            }
            if (root.hasNonNull("segmentEnd")) {
                summary.setSegmentEnd(root.get("segmentEnd").asLong());
            }
            if (root.hasNonNull("segmentSize")) {
                summary.setSegmentSize(root.get("segmentSize").asInt());
            }
            if (root.hasNonNull("rewardSeed")) {
                summary.setRewardSeed(root.get("rewardSeed").asLong());
            }
            JsonNode roundPlanList = root.get("roundPlanList");
            if (roundPlanList != null && roundPlanList.isArray()) {
                summary.setLockedRoundCount(roundPlanList.size());
            }
        } catch (Exception e) {
            throw new IllegalStateException("解析 currentSegmentRewardPlanJson 失败", e);
        }
    }

    /**
     * 组装当前 pending_reward_pool 视图。
     *
     * @param pendingData pending 数据
     * @return pending 视图
     */
    private QueryGatherTaskPanelResult.PendingRewardPoolView buildPendingRewardPoolView(PendingRewardData pendingData) {
        if (pendingData == null || !pendingData.hasAnyData()) {
            return QueryGatherTaskPanelResult.PendingRewardPoolView.createEmpty();
        }

        Map<String, String> rewardNameMap = loadRewardNameMap(pendingData.getRewardList());
        List<QueryGatherTaskPanelResult.RewardEntryView> rewardViewList = new ArrayList<QueryGatherTaskPanelResult.RewardEntryView>();
        for (int i = 0; i < pendingData.getRewardList().size(); i++) {
            RewardAmount rewardAmount = pendingData.getRewardList().get(i);
            rewardViewList.add(gatherTaskViewAssembler.buildRewardEntryView(
                    rewardAmount.getRewardType(),
                    rewardAmount.getRewardCode(),
                    rewardNameMap.get(buildRewardKey(rewardAmount.getRewardType(), rewardAmount.getRewardCode())),
                    rewardAmount.getQuantity()
            ));
        }
        return gatherTaskViewAssembler.buildPendingRewardPoolView(
                pendingData.getTaskId(),
                pendingData.getCompletedCount(),
                pendingData.getFlushedCount(),
                pendingData.getPendingRoundCount(),
                rewardViewList
        );
    }

    /**
     * 组装采集队列视图列表。
     *
     * @param queueEntityList 队列实体列表
     * @param actionNameMap 动作名称映射
     * @return 队列视图列表
     */
    private List<QueryGatherTaskPanelResult.QueueItemView> buildQueueViewList(List<PlayerActionQueueEntity> queueEntityList,
                                                                              Map<String, String> actionNameMap) {
        if (queueEntityList == null || queueEntityList.isEmpty()) {
            return new ArrayList<QueryGatherTaskPanelResult.QueueItemView>();
        }

        List<QueryGatherTaskPanelResult.QueueItemView> result = new ArrayList<QueryGatherTaskPanelResult.QueueItemView>();
        for (int i = 0; i < queueEntityList.size(); i++) {
            PlayerActionQueueEntity entity = queueEntityList.get(i);
            QueryGatherTaskPanelResult.QueueItemView view = gatherTaskViewAssembler.buildQueueItemView(
                    entity,
                    i + 1,
                    actionNameMap.get(entity.getActionCode())
            );
            result.add(view);
        }
        return result;
    }

    /**
     * 组装展示库存视图。
     *
     * @param playerId 角色ID
     * @param pendingInventoryMap pending 聚合库存
     * @return 展示库存视图
     */
    private QueryGatherTaskPanelResult.InventoryView buildInventoryView(Long playerId, Map<String, Long> pendingInventoryMap) {
        Map<String, Long> formalInventoryMap = buildFormalInventoryMap(playerId);
        Map<String, String> rewardNameMap = loadRewardNameMap(formalInventoryMap.keySet(), pendingInventoryMap.keySet());

        Set<String> allKeys = new LinkedHashSet<String>();
        allKeys.addAll(formalInventoryMap.keySet());
        allKeys.addAll(pendingInventoryMap.keySet());

        List<String> sortedKeys = new ArrayList<String>(allKeys);
        Collections.sort(sortedKeys);

        List<QueryGatherTaskPanelResult.InventoryEntryView> entryList = new ArrayList<QueryGatherTaskPanelResult.InventoryEntryView>();
        for (int i = 0; i < sortedKeys.size(); i++) {
            String key = sortedKeys.get(i);
            String rewardType = getRewardTypeFromKey(key);
            String rewardCode = getRewardCodeFromKey(key);
            Long formalQuantity = formalInventoryMap.get(key);
            Long pendingQuantity = pendingInventoryMap.get(key);
            String rewardName = rewardNameMap.get(key);
            entryList.add(gatherTaskViewAssembler.buildInventoryEntryView(
                    rewardType,
                    rewardCode,
                    rewardName,
                    formalQuantity,
                    pendingQuantity
            ));
        }
        return gatherTaskViewAssembler.buildInventoryView(entryList);
    }

    /**
     * 汇总正式库存。
     *
     * 说明：
     * 1. 钱包直接读取 t_urr_wallet。
     * 2. 物品直接读取 t_urr_player_item_stack。
     * 3. 当前先聚合角色下全部正式物品数量，不区分 location 展示。
     *
     * @param playerId 角色ID
     * @return 正式库存聚合结果
     */
    private Map<String, Long> buildFormalInventoryMap(Long playerId) {
        Map<String, Long> result = new LinkedHashMap<String, Long>();

        List<WalletEntity> walletList = walletMapper.selectList(
                new LambdaQueryWrapper<WalletEntity>()
                        .eq(WalletEntity::getPlayerId, playerId)
                        .gt(WalletEntity::getBalance, 0L)
                        .orderByAsc(WalletEntity::getCurrencyCode)
        );
        for (int i = 0; i < walletList.size(); i++) {
            WalletEntity wallet = walletList.get(i);
            mergeAmount(result, buildRewardKey("CURRENCY", wallet.getCurrencyCode()), wallet.getBalance());
        }

        List<PlayerItemStackEntity> itemStackList = playerItemStackMapper.selectList(
                new LambdaQueryWrapper<PlayerItemStackEntity>()
                        .eq(PlayerItemStackEntity::getPlayerId, playerId)
                        .gt(PlayerItemStackEntity::getQty, 0L)
                        .orderByAsc(PlayerItemStackEntity::getItemId)
        );
        Map<Long, Long> itemQtyMap = new LinkedHashMap<Long, Long>();
        for (int i = 0; i < itemStackList.size(); i++) {
            PlayerItemStackEntity itemStack = itemStackList.get(i);
            Long oldQty = itemQtyMap.get(itemStack.getItemId());
            if (oldQty == null) {
                oldQty = 0L;
            }
            itemQtyMap.put(itemStack.getItemId(), oldQty + safeLong(itemStack.getQty()));
        }

        if (!itemQtyMap.isEmpty()) {
            List<ItemDefEntity> itemDefList = itemDefMapper.selectBatchIds(itemQtyMap.keySet());
            for (int i = 0; i < itemDefList.size(); i++) {
                ItemDefEntity itemDef = itemDefList.get(i);
                Long qty = itemQtyMap.get(itemDef.getId());
                mergeAmount(result, buildRewardKey("ITEM", itemDef.getItemCode()), qty);
            }
        }
        return result;
    }

    /**
     * 汇总 pending_reward_pool 作为展示库存的一部分。
     *
     * @param pendingTaskList 有 pending 的采集任务列表
     * @return pending 聚合库存
     */
    private Map<String, Long> buildPendingInventoryMap(List<PlayerGatherTask> pendingTaskList) {
        Map<String, Long> result = new LinkedHashMap<String, Long>();
        if (pendingTaskList == null || pendingTaskList.isEmpty()) {
            return result;
        }
        for (int i = 0; i < pendingTaskList.size(); i++) {
            PendingRewardData data = loadPendingRewardDataByTask(pendingTaskList.get(i));
            for (int j = 0; j < data.getRewardList().size(); j++) {
                RewardAmount rewardAmount = data.getRewardList().get(j);
                mergeAmount(result, buildRewardKey(rewardAmount.getRewardType(), rewardAmount.getRewardCode()), rewardAmount.getQuantity());
            }
        }
        return result;
    }

    /**
     * 批量读取动作名称映射。
     *
     * @param currentRunningTask 当前运行任务
     * @param currentOrLatestGatherTask 当前或最近任务
     * @param queueEntityList 队列实体列表
     * @param pendingTaskList pending 任务列表
     * @return 动作名称映射
     */
    private Map<String, String> loadActionNameMap(PlayerGatherTask currentRunningTask,
                                                  PlayerGatherTask currentOrLatestGatherTask,
                                                  List<PlayerActionQueueEntity> queueEntityList,
                                                  List<PlayerGatherTask> pendingTaskList) {
        Set<String> actionCodeSet = new LinkedHashSet<String>();
        if (currentRunningTask != null && StringUtils.hasText(currentRunningTask.getActionCode())) {
            actionCodeSet.add(currentRunningTask.getActionCode());
        }
        if (currentOrLatestGatherTask != null && StringUtils.hasText(currentOrLatestGatherTask.getActionCode())) {
            actionCodeSet.add(currentOrLatestGatherTask.getActionCode());
        }
        if (queueEntityList != null) {
            for (int i = 0; i < queueEntityList.size(); i++) {
                PlayerActionQueueEntity entity = queueEntityList.get(i);
                if (StringUtils.hasText(entity.getActionCode())) {
                    actionCodeSet.add(entity.getActionCode());
                }
            }
        }
        if (pendingTaskList != null) {
            for (int i = 0; i < pendingTaskList.size(); i++) {
                PlayerGatherTask task = pendingTaskList.get(i);
                if (task != null && StringUtils.hasText(task.getActionCode())) {
                    actionCodeSet.add(task.getActionCode());
                }
            }
        }
        if (actionCodeSet.isEmpty()) {
            return new HashMap<String, String>();
        }

        List<ActionDefEntity> actionDefList = actionDefMapper.selectList(
                new LambdaQueryWrapper<ActionDefEntity>()
                        .in(ActionDefEntity::getActionCode, actionCodeSet)
        );
        Map<String, String> result = new HashMap<String, String>();
        for (int i = 0; i < actionDefList.size(); i++) {
            ActionDefEntity actionDef = actionDefList.get(i);
            result.put(actionDef.getActionCode(), actionDef.getActionName());
        }
        return result;
    }

    /**
     * 读取奖励名称映射。
     *
     * @param rewardList 奖励列表
     * @return 奖励名称映射
     */
    private Map<String, String> loadRewardNameMap(List<RewardAmount> rewardList) {
        Set<String> rewardKeySet = new LinkedHashSet<String>();
        if (rewardList != null) {
            for (int i = 0; i < rewardList.size(); i++) {
                RewardAmount rewardAmount = rewardList.get(i);
                rewardKeySet.add(buildRewardKey(rewardAmount.getRewardType(), rewardAmount.getRewardCode()));
            }
        }
        return loadRewardNameMap(rewardKeySet, Collections.<String>emptySet());
    }

    /**
     * 读取奖励名称映射。
     *
     * @param firstKeys 第一批 key
     * @param secondKeys 第二批 key
     * @return 奖励名称映射
     */
    private Map<String, String> loadRewardNameMap(Set<String> firstKeys, Set<String> secondKeys) {
        Set<String> allKeys = new LinkedHashSet<String>();
        if (firstKeys != null) {
            allKeys.addAll(firstKeys);
        }
        if (secondKeys != null) {
            allKeys.addAll(secondKeys);
        }

        Set<String> itemCodeSet = new LinkedHashSet<String>();
        Set<String> currencyCodeSet = new LinkedHashSet<String>();
        for (String key : allKeys) {
            String rewardType = getRewardTypeFromKey(key);
            String rewardCode = getRewardCodeFromKey(key);
            if ("ITEM".equalsIgnoreCase(rewardType)) {
                itemCodeSet.add(rewardCode);
            } else if ("CURRENCY".equalsIgnoreCase(rewardType)) {
                currencyCodeSet.add(rewardCode);
            }
        }

        Map<String, String> result = new HashMap<String, String>();
        if (!itemCodeSet.isEmpty()) {
            List<ItemDefEntity> itemDefList = itemDefMapper.selectList(
                    new LambdaQueryWrapper<ItemDefEntity>()
                            .in(ItemDefEntity::getItemCode, itemCodeSet)
            );
            for (int i = 0; i < itemDefList.size(); i++) {
                ItemDefEntity itemDef = itemDefList.get(i);
                result.put(buildRewardKey("ITEM", itemDef.getItemCode()), itemDef.getNameZh());
            }
        }

        if (!currencyCodeSet.isEmpty()) {
            List<ItemDefEntity> currencyItemDefList = itemDefMapper.selectList(
                    new LambdaQueryWrapper<ItemDefEntity>()
                            .eq(ItemDefEntity::getItemType, 6)
            );
            for (int i = 0; i < currencyItemDefList.size(); i++) {
                ItemDefEntity itemDef = currencyItemDefList.get(i);
                String currencyCode = extractCurrencyCode(itemDef.getMetaJson());
                if (!StringUtils.hasText(currencyCode)) {
                    continue;
                }
                if (!currencyCodeSet.contains(currencyCode)) {
                    continue;
                }
                result.put(buildRewardKey("CURRENCY", currencyCode), itemDef.getNameZh());
            }
        }

        for (String key : allKeys) {
            if (!result.containsKey(key)) {
                result.put(key, getRewardCodeFromKey(key));
            }
        }
        return result;
    }

    /**
     * 从奖励池 JSON 中解析奖励列表。
     *
     * @param rewardPoolJson 奖励池 JSON
     * @return 奖励列表
     */
    private List<RewardAmount> parseRewardList(String rewardPoolJson) {
        List<RewardAmount> result = new ArrayList<RewardAmount>();
        if (!StringUtils.hasText(rewardPoolJson)) {
            return result;
        }
        try {
            JsonNode root = objectMapper.readTree(rewardPoolJson);
            JsonNode rewardList = root.get("rewardList");
            if (rewardList == null || !rewardList.isArray()) {
                return result;
            }
            for (int i = 0; i < rewardList.size(); i++) {
                JsonNode rewardNode = rewardList.get(i);
                RewardAmount rewardAmount = new RewardAmount();
                rewardAmount.setRewardType(getText(rewardNode, "rewardType"));
                rewardAmount.setRewardCode(getText(rewardNode, "rewardCode"));
                rewardAmount.setQuantity(getLong(rewardNode, "quantity"));
                if (!StringUtils.hasText(rewardAmount.getRewardType()) || !StringUtils.hasText(rewardAmount.getRewardCode())) {
                    continue;
                }
                result.add(rewardAmount);
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("解析 pendingRewardPoolJson 失败", e);
        }
    }

    /**
     * 从 metaJson 中提取 currency_code。
     *
     * @param metaJson 物品元数据 JSON
     * @return 货币编码
     */
    private String extractCurrencyCode(String metaJson) {
        if (!StringUtils.hasText(metaJson)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(metaJson);
            if (!root.hasNonNull("currency_code")) {
                return null;
            }
            return root.get("currency_code").asText();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 合并数量到聚合 Map。
     *
     * @param amountMap 数量 Map
     * @param key 聚合 key
     * @param quantity 数量
     */
    private void mergeAmount(Map<String, Long> amountMap, String key, Long quantity) {
        if (amountMap == null || !StringUtils.hasText(key) || quantity == null || quantity <= 0L) {
            return;
        }
        Long oldValue = amountMap.get(key);
        if (oldValue == null) {
            oldValue = 0L;
        }
        amountMap.put(key, oldValue + quantity);
    }

    /**
     * 构建奖励聚合 key。
     *
     * @param rewardType 奖励类型
     * @param rewardCode 奖励编码
     * @return 聚合 key
     */
    private String buildRewardKey(String rewardType, String rewardCode) {
        return safeText(rewardType).toUpperCase() + "|" + safeText(rewardCode);
    }

    /**
     * 从聚合 key 中提取奖励类型。
     *
     * @param key 聚合 key
     * @return 奖励类型
     */
    private String getRewardTypeFromKey(String key) {
        int index = key.indexOf('|');
        if (index < 0) {
            return key;
        }
        return key.substring(0, index);
    }

    /**
     * 从聚合 key 中提取奖励编码。
     *
     * @param key 聚合 key
     * @return 奖励编码
     */
    private String getRewardCodeFromKey(String key) {
        int index = key.indexOf('|');
        if (index < 0 || index >= key.length() - 1) {
            return "";
        }
        return key.substring(index + 1);
    }

    /**
     * 安全读取 JSON 字段文本。
     *
     * @param node JSON 节点
     * @param field 字段名
     * @return 文本值
     */
    private String getText(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        return node.get(field).asText();
    }

    /**
     * 安全读取 JSON 字段 long 值。
     *
     * @param node JSON 节点
     * @param field 字段名
     * @return long 值
     */
    private Long getLong(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return 0L;
        }
        return node.get(field).asLong();
    }

    /**
     * 安全文本处理。
     *
     * @param text 文本
     * @return 非 null 文本
     */
    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }

    /**
     * 安全 long 处理。
     *
     * @param value 原值
     * @return 非 null long
     */
    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * pending 数据。
     */
    @Data
    private static class PendingRewardData {

        private Long taskId;
        private Long completedCount;
        private Long flushedCount;
        private Long pendingRoundCount;
        private List<RewardAmount> rewardList = new ArrayList<RewardAmount>();

        /**
         * 创建空 pending 数据。
         *
         * @return 空 pending 数据
         */
        public static PendingRewardData empty() {
            PendingRewardData data = new PendingRewardData();
            data.setPendingRoundCount(0L);
            return data;
        }

        /**
         * 判断当前是否存在 pending。
         *
         * @return true-存在，false-不存在
         */
        public boolean hasPending() {
            return (pendingRoundCount != null && pendingRoundCount > 0L) || (rewardList != null && !rewardList.isEmpty());
        }

        /**
         * 判断当前是否带有任何可展示数据。
         *
         * @return true-有数据，false-无数据
         */
        public boolean hasAnyData() {
            return taskId != null || hasPending();
        }
    }

    /**
     * 分段摘要。
     */
    @Data
    private static class SegmentPlanSummary {

        private Long segmentStart;
        private Long segmentEnd;
        private Integer segmentSize;
        private Long rewardSeed;
        private Integer lockedRoundCount;
    }

    /**
     * 奖励数量对象。
     */
    @Data
    private static class RewardAmount {

        private String rewardType;
        private String rewardCode;
        private Long quantity;
    }
}