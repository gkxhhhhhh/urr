package com.urr.app.action.task;

import com.urr.app.action.task.result.QueryGatherTaskPanelResult;
import com.urr.domain.action.task.GatherTaskStatSnapshot;
import com.urr.domain.action.task.PlayerActionQueueEntity;
import com.urr.domain.action.task.PlayerGatherTask;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 采集任务视图组装器。
 *
 * 说明：
 * 1. 这里只做“领域/缓存对象 -> 读结果对象”的轻量转换。
 * 2. 不负责查询，不负责推进，不负责库存汇总。
 */
@Component
public class GatherTaskViewAssembler {

    /**
     * 组装顶层面板结果。
     *
     * @param playerId 角色ID
     * @param readTime 读取时间
     * @param currentTask 当前运行任务
     * @param queueList 队列
     * @param pendingRewardPool 当前 pending 视图
     * @param inventoryView 展示库存
     * @return 面板结果
     */
    public QueryGatherTaskPanelResult buildPanelResult(Long playerId,
                                                       java.time.LocalDateTime readTime,
                                                       QueryGatherTaskPanelResult.RuntimeView currentTask,
                                                       List<QueryGatherTaskPanelResult.QueueItemView> queueList,
                                                       QueryGatherTaskPanelResult.PendingRewardPoolView pendingRewardPool,
                                                       QueryGatherTaskPanelResult.InventoryView inventoryView) {
        QueryGatherTaskPanelResult result = QueryGatherTaskPanelResult.createEmpty(playerId, readTime);
        result.setHasRunningTask(currentTask != null);
        result.setCurrentTask(currentTask);
        result.setQueueList(queueList == null ? new ArrayList<QueryGatherTaskPanelResult.QueueItemView>() : queueList);
        result.setQueueSize(result.getQueueList().size());
        result.setPendingRewardPool(pendingRewardPool == null ? QueryGatherTaskPanelResult.PendingRewardPoolView.createEmpty() : pendingRewardPool);
        result.setDisplayInventory(inventoryView == null ? QueryGatherTaskPanelResult.InventoryView.createEmpty() : inventoryView);
        return result;
    }

    /**
     * 组装运行中任务视图。
     *
     * @param task 采集任务
     * @param actionName 动作名称
     * @param pendingView 当前 pending 视图
     * @param segmentView 当前分段视图
     * @return 运行中任务视图
     */
    public QueryGatherTaskPanelResult.RuntimeView buildRuntimeView(PlayerGatherTask task,
                                                                   String actionName,
                                                                   QueryGatherTaskPanelResult.PendingRewardPoolView pendingView,
                                                                   QueryGatherTaskPanelResult.SegmentView segmentView) {
        if (task == null) {
            return null;
        }

        QueryGatherTaskPanelResult.RuntimeView view = new QueryGatherTaskPanelResult.RuntimeView();
        view.setTaskId(task.getId());
        view.setActionCode(task.getActionCode());
        view.setActionName(actionName);
        view.setStatus(task.getStatus() == null ? null : task.getStatus().getCode());
        view.setTargetCount(task.getTargetCount());
        view.setInfiniteTarget(task.isInfiniteTarget());
        view.setCompletedCount(task.getCompletedCount());
        view.setFlushedCount(task.getFlushedCount());
        view.setRemainingCount(calculateRemainingCount(task));
        view.setCurrentSegmentStart(task.getCurrentSegmentStart());
        view.setCurrentSegmentEnd(task.getCurrentSegmentEnd());
        view.setSegmentSize(task.getSegmentSize());
        view.setStartTime(task.getStartTime());
        view.setLastSettleTime(task.getLastSettleTime());
        view.setOfflineExpireAt(task.getOfflineExpireAt());
        view.setSnapshot(buildSnapshotView(task.getStatSnapshot()));
        view.setCurrentSegment(segmentView);
        view.setPendingRewardPool(pendingView == null ? QueryGatherTaskPanelResult.PendingRewardPoolView.createEmpty() : pendingView);
        return view;
    }

    /**
     * 组装采集快照最小视图。
     *
     * @param snapshot 采集快照
     * @return 快照视图
     */
    public QueryGatherTaskPanelResult.SnapshotView buildSnapshotView(GatherTaskStatSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }

        QueryGatherTaskPanelResult.SnapshotView view = new QueryGatherTaskPanelResult.SnapshotView();
        view.setGatherLevel(snapshot.getGatherLevel());
        view.setGatherDurationMs(snapshot.getGatherDurationMs());
        view.setGatherEfficiency(snapshot.getGatherEfficiency() == null ? null : snapshot.getGatherEfficiency().toPlainString());
        view.setOfflineMinutesLimit(snapshot.getOfflineMinutesLimit());
        return view;
    }

    /**
     * 组装当前分段最小视图。
     *
     * @param segmentStart 分段开始
     * @param segmentEnd 分段结束
     * @param segmentSize 分段大小
     * @param rewardSeed 奖励种子
     * @param lockedRoundCount 锁定轮次数
     * @param completedCount 当前已完成轮次
     * @return 当前分段视图
     */
    public QueryGatherTaskPanelResult.SegmentView buildSegmentView(Long segmentStart,
                                                                   Long segmentEnd,
                                                                   Integer segmentSize,
                                                                   Long rewardSeed,
                                                                   Integer lockedRoundCount,
                                                                   Long completedCount) {
        if (segmentStart == null && segmentEnd == null && segmentSize == null && rewardSeed == null && lockedRoundCount == null) {
            return null;
        }

        QueryGatherTaskPanelResult.SegmentView view = new QueryGatherTaskPanelResult.SegmentView();
        view.setSegmentStart(segmentStart);
        view.setSegmentEnd(segmentEnd);
        view.setSegmentSize(segmentSize);
        view.setRewardSeed(rewardSeed);
        view.setLockedRoundCount(lockedRoundCount);
        view.setCompletedCountInSegment(calculateCompletedCountInSegment(segmentStart, segmentEnd, completedCount));
        view.setRemainingCountInSegment(calculateRemainingCountInSegment(segmentStart, segmentEnd, completedCount));
        return view;
    }

    /**
     * 组装 pending_reward_pool 视图。
     *
     * @param taskId 任务ID
     * @param completedCount 已完成轮次
     * @param flushedCount 已刷库轮次
     * @param pendingRoundCount 待刷轮次
     * @param rewardList 奖励列表
     * @return pending 视图
     */
    public QueryGatherTaskPanelResult.PendingRewardPoolView buildPendingRewardPoolView(Long taskId,
                                                                                       Long completedCount,
                                                                                       Long flushedCount,
                                                                                       Long pendingRoundCount,
                                                                                       List<QueryGatherTaskPanelResult.RewardEntryView> rewardList) {
        QueryGatherTaskPanelResult.PendingRewardPoolView view = QueryGatherTaskPanelResult.PendingRewardPoolView.createEmpty();
        view.setTaskId(taskId);
        view.setCompletedCount(completedCount);
        view.setFlushedCount(flushedCount);
        view.setPendingRoundCount(pendingRoundCount == null ? 0L : pendingRoundCount);
        view.setRewardList(rewardList == null ? new ArrayList<QueryGatherTaskPanelResult.RewardEntryView>() : rewardList);
        view.setHasPending(view.getPendingRoundCount() > 0 || !view.getRewardList().isEmpty());
        return view;
    }

    /**
     * 组装一个奖励项视图。
     *
     * @param rewardType 奖励类型
     * @param rewardCode 奖励编码
     * @param rewardName 奖励名称
     * @param quantity 数量
     * @return 奖励项视图
     */
    public QueryGatherTaskPanelResult.RewardEntryView buildRewardEntryView(String rewardType,
                                                                           String rewardCode,
                                                                           String rewardName,
                                                                           Long quantity) {
        QueryGatherTaskPanelResult.RewardEntryView view = new QueryGatherTaskPanelResult.RewardEntryView();
        view.setRewardType(rewardType);
        view.setRewardCode(rewardCode);
        view.setRewardName(rewardName);
        view.setQuantity(quantity == null ? 0L : quantity);
        return view;
    }

    /**
     * 组装一个队列项视图。
     *
     * @param entity 队列实体
     * @param queuePosition 排队位置
     * @param actionName 动作名称
     * @return 队列项视图
     */
    public QueryGatherTaskPanelResult.QueueItemView buildQueueItemView(PlayerActionQueueEntity entity,
                                                                       Integer queuePosition,
                                                                       String actionName) {
        if (entity == null) {
            return null;
        }

        QueryGatherTaskPanelResult.QueueItemView view = new QueryGatherTaskPanelResult.QueueItemView();
        view.setQueueId(entity.getId());
        view.setTaskId(null);
        view.setActionCode(entity.getActionCode());
        view.setActionName(actionName);
        view.setStatus(entity.getStatus());
        view.setQueuePosition(queuePosition);
        view.setTargetCount(entity.getTargetCount());
        view.setInfiniteTarget(entity.getTargetCount() != null && entity.getTargetCount() == -1L);
        return view;
    }

    /**
     * 组装展示库存视图。
     *
     * @param entryList 展示库存项
     * @return 展示库存视图
     */
    public QueryGatherTaskPanelResult.InventoryView buildInventoryView(List<QueryGatherTaskPanelResult.InventoryEntryView> entryList) {
        QueryGatherTaskPanelResult.InventoryView view = QueryGatherTaskPanelResult.InventoryView.createEmpty();
        if (entryList == null) {
            return view;
        }
        view.setEntryCount(entryList.size());
        view.setEntryList(entryList);
        return view;
    }

    /**
     * 组装一个展示库存项。
     *
     * @param rewardType 奖励类型
     * @param rewardCode 奖励编码
     * @param rewardName 奖励名称
     * @param formalQuantity 正式库存数量
     * @param pendingQuantity 待入库数量
     * @return 展示库存项
     */
    public QueryGatherTaskPanelResult.InventoryEntryView buildInventoryEntryView(String rewardType,
                                                                                 String rewardCode,
                                                                                 String rewardName,
                                                                                 Long formalQuantity,
                                                                                 Long pendingQuantity) {
        QueryGatherTaskPanelResult.InventoryEntryView view = new QueryGatherTaskPanelResult.InventoryEntryView();
        long safeFormalQuantity = formalQuantity == null ? 0L : formalQuantity;
        long safePendingQuantity = pendingQuantity == null ? 0L : pendingQuantity;
        view.setRewardType(rewardType);
        view.setRewardCode(rewardCode);
        view.setRewardName(rewardName);
        view.setFormalQuantity(safeFormalQuantity);
        view.setPendingQuantity(safePendingQuantity);
        view.setDisplayQuantity(safeFormalQuantity + safePendingQuantity);
        return view;
    }

    /**
     * 计算剩余轮次。
     *
     * @param task 采集任务
     * @return 剩余轮次；无限任务返回 null
     */
    private Long calculateRemainingCount(PlayerGatherTask task) {
        if (task == null || task.isInfiniteTarget()) {
            return null;
        }
        if (task.getTargetCount() == null) {
            return 0L;
        }
        long remaining = task.getTargetCount() - task.getSafeCompletedCount();
        return Math.max(remaining, 0L);
    }

    /**
     * 计算当前分段内已完成轮次。
     *
     * @param segmentStart 分段开始
     * @param segmentEnd 分段结束
     * @param completedCount 总已完成轮次
     * @return 当前分段内已完成轮次
     */
    private Long calculateCompletedCountInSegment(Long segmentStart, Long segmentEnd, Long completedCount) {
        if (segmentStart == null || segmentEnd == null) {
            return 0L;
        }
        long safeCompletedCount = completedCount == null ? 0L : completedCount;
        if (safeCompletedCount < segmentStart) {
            return 0L;
        }
        long completedInSegment = Math.min(safeCompletedCount, segmentEnd) - segmentStart + 1;
        return Math.max(completedInSegment, 0L);
    }

    /**
     * 计算当前分段内剩余轮次。
     *
     * @param segmentStart 分段开始
     * @param segmentEnd 分段结束
     * @param completedCount 总已完成轮次
     * @return 当前分段内剩余轮次
     */
    private Long calculateRemainingCountInSegment(Long segmentStart, Long segmentEnd, Long completedCount) {
        if (segmentStart == null || segmentEnd == null) {
            return 0L;
        }
        long total = segmentEnd - segmentStart + 1;
        long completedInSegment = calculateCompletedCountInSegment(segmentStart, segmentEnd, completedCount);
        long remaining = total - completedInSegment;
        return Math.max(remaining, 0L);
    }
}