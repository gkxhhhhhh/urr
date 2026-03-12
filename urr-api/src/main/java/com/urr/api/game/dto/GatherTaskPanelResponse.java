package com.urr.api.game.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 采集面板响应。
 *
 * 说明：
 * 1. 这里只承载前端最小接入需要的稳定字段。
 * 2. 不把 entity / cache 直接暴露给前端。
 */
@Data
public class GatherTaskPanelResponse {

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
     * 当前队列大小。
     */
    private Integer queueSize;

    /**
     * 当前运行中的采集任务。
     */
    private RunningTask runningTask;

    /**
     * 当前采集队列。
     */
    private List<QueueItem> queueList;

    /**
     * 当前 pending_reward_pool。
     */
    private PendingRewardPool pendingRewardPool;

    /**
     * 展示库存。
     */
    private Inventory displayInventory;

    /**
     * 运行中任务视图。
     */
    @Data
    public static class RunningTask {

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
        private Snapshot snapshot;
        private Segment currentSegment;
        private PendingRewardPool pendingRewardPool;
    }

    /**
     * 采集快照视图。
     */
    @Data
    public static class Snapshot {

        private Integer gatherLevel;
        private Integer gatherDurationMs;
        private String gatherEfficiency;
        private Integer offlineMinutesLimit;
    }

    /**
     * 当前分段视图。
     */
    @Data
    public static class Segment {

        private Long segmentStart;
        private Long segmentEnd;
        private Integer segmentSize;
        private Long rewardSeed;
        private Integer lockedRoundCount;
        private Long completedCountInSegment;
        private Long remainingCountInSegment;
    }

    /**
     * pending_reward_pool 视图。
     */
    @Data
    public static class PendingRewardPool {

        private Long taskId;
        private Long completedCount;
        private Long flushedCount;
        private Long pendingRoundCount;
        private Boolean hasPending;
        private List<RewardEntry> rewardList;
    }

    /**
     * 队列项视图。
     */
    @Data
    public static class QueueItem {

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
    public static class Inventory {

        private Integer entryCount;
        private List<InventoryEntry> entryList;
    }

    /**
     * 展示库存项视图。
     */
    @Data
    public static class InventoryEntry {

        private String rewardType;
        private String rewardCode;
        private String rewardName;
        private Long formalQuantity;
        private Long pendingQuantity;
        private Long displayQuantity;
    }

    /**
     * 奖励项视图。
     */
    @Data
    public static class RewardEntry {

        private String rewardType;
        private String rewardCode;
        private String rewardName;
        private Long quantity;
    }
}