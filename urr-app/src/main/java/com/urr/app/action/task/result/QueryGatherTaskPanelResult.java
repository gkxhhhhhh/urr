package com.urr.app.action.task.result;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 采集面板最小读结果。
 *
 * 说明：
 * 1. 这里只服务“当前运行任务 / 队列 / pending_reward_pool / 展示库存”。
 * 2. 当前不承载 Controller 语义，只作为 app 层对外结果对象。
 * 3. 结构保持平直，便于后续前端面板直接接入。
 */
@Data
public class QueryGatherTaskPanelResult {

    /**
     * 角色ID。
     */
    private Long playerId;

    /**
     * 本次读取时间。
     */
    private LocalDateTime readTime;

    /**
     * 当前是否存在运行中的采集任务。
     */
    private Boolean hasRunningTask;

    /**
     * 当前采集队列大小。
     */
    private Integer queueSize;

    /**
     * 当前运行中的采集任务。
     */
    private RuntimeView currentTask;

    /**
     * 当前采集队列。
     */
    private List<QueueItemView> queueList;

    /**
     * 当前 pending_reward_pool。
     */
    private PendingRewardPoolView pendingRewardPool;

    /**
     * 展示库存。
     */
    private InventoryView displayInventory;

    /**
     * 创建一个空面板结果。
     *
     * @param playerId 角色ID
     * @param readTime 读取时间
     * @return 空结果
     */
    public static QueryGatherTaskPanelResult createEmpty(Long playerId, LocalDateTime readTime) {
        QueryGatherTaskPanelResult result = new QueryGatherTaskPanelResult();
        result.setPlayerId(playerId);
        result.setReadTime(readTime);
        result.setHasRunningTask(Boolean.FALSE);
        result.setQueueSize(0);
        result.setQueueList(new ArrayList<QueueItemView>());
        result.setPendingRewardPool(PendingRewardPoolView.createEmpty());
        result.setDisplayInventory(InventoryView.createEmpty());
        return result;
    }

    /**
     * 运行中采集任务视图。
     */
    @Data
    public static class RuntimeView {

        private Long taskId;
        private String actionCode;
        private String actionName;
        private String status;
        private Long targetCount;
        private Boolean infiniteTarget;
        private Long completedCount;
        private Long flushedCount;
        private Long remainingCount;
        private Long currentSegmentStart;
        private Long currentSegmentEnd;
        private Integer segmentSize;
        private LocalDateTime startTime;
        private LocalDateTime lastSettleTime;
        private LocalDateTime offlineExpireAt;
        private SnapshotView snapshot;
        private SegmentView currentSegment;
        private PendingRewardPoolView pendingRewardPool;
    }

    /**
     * 采集快照最小视图。
     */
    @Data
    public static class SnapshotView {

        private Integer gatherLevel;
        private Integer gatherDurationMs;
        private String gatherEfficiency;
        private Integer offlineMinutesLimit;
    }

    /**
     * 当前分段最小视图。
     */
    @Data
    public static class SegmentView {

        private Long segmentStart;
        private Long segmentEnd;
        private Integer segmentSize;
        private Long rewardSeed;
        private Integer lockedRoundCount;
        private Long completedCountInSegment;
        private Long remainingCountInSegment;
    }

    /**
     * pending_reward_pool 最小视图。
     */
    @Data
    public static class PendingRewardPoolView {

        private Long taskId;
        private Long completedCount;
        private Long flushedCount;
        private Long pendingRoundCount;
        private Boolean hasPending;
        private List<RewardEntryView> rewardList;

        /**
         * 创建空 pending 视图。
         *
         * @return 空 pending 视图
         */
        public static PendingRewardPoolView createEmpty() {
            PendingRewardPoolView view = new PendingRewardPoolView();
            view.setPendingRoundCount(0L);
            view.setHasPending(Boolean.FALSE);
            view.setRewardList(new ArrayList<RewardEntryView>());
            return view;
        }
    }

    /**
     * 队列项最小视图。
     */
    @Data
    public static class QueueItemView {

        private Long queueId;
        private Long taskId;
        private String actionCode;
        private String actionName;
        private String status;
        private Integer queuePosition;
        private Long targetCount;
        private Boolean infiniteTarget;
    }

    /**
     * 展示库存视图。
     */
    @Data
    public static class InventoryView {

        private Integer entryCount;
        private List<InventoryEntryView> entryList;

        /**
         * 创建空展示库存。
         *
         * @return 空展示库存
         */
        public static InventoryView createEmpty() {
            InventoryView view = new InventoryView();
            view.setEntryCount(0);
            view.setEntryList(new ArrayList<InventoryEntryView>());
            return view;
        }
    }

    /**
     * 展示库存项视图。
     */
    @Data
    public static class InventoryEntryView {

        private String rewardType;
        private String rewardCode;
        private String rewardName;
        private Long formalQuantity;
        private Long pendingQuantity;
        private Long displayQuantity;
        private Long itemId;
        private Long equipInstanceId;
        private Integer itemType;
        private Integer itemLevel;
        private Integer strengthenLevel;
        private String equipCategory;
        private Double baseAttack;
        private Double currentAttack;
    }

    /**
     * 奖励项最小视图。
     */
    @Data
    public static class RewardEntryView {

        private String rewardType;
        private String rewardCode;
        private String rewardName;
        private Long quantity;
    }
}