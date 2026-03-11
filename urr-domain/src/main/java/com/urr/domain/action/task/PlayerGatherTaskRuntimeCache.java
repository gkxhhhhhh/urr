package com.urr.domain.action.task;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采集任务运行态热缓存。
 *
 * 说明：
 * 1. 这个对象承载“当前采集任务正在运行时”最常用的热态字段。
 * 2. 它是数据库真相层的运行态镜像，不是唯一真相。
 * 3. currentSegmentRewardPlanJson 和 pendingRewardPoolJson 不放在这里，
 *    而是拆到独立 key，避免一个对象过大，也方便后续按职责演进。
 */
@Data
public class PlayerGatherTaskRuntimeCache {

    /**
     * 任务ID。
     */
    private Long taskId;

    /**
     * 玩家ID。
     */
    private Long playerId;

    /**
     * 区服ID。
     */
    private Integer serverId;

    /**
     * 动作编码。
     */
    private String actionCode;

    /**
     * 任务业务类型。
     */
    private String taskType;

    /**
     * 任务状态。
     */
    private String status;

    /**
     * 任务停止原因。
     */
    private String stopReason;

    /**
     * 任务开始时间。
     */
    private LocalDateTime startTime;

    /**
     * 最近一次交互时间。
     */
    private LocalDateTime lastInteractTime;

    /**
     * 最近一次任务层结算时间。
     */
    private LocalDateTime lastSettleTime;

    /**
     * 离线截止时间。
     */
    private LocalDateTime offlineExpireAt;

    /**
     * 奖励随机种子。
     */
    private Long rewardSeed;

    /**
     * 目标轮次。
     * -1 表示无限次数。
     */
    private Long targetCount;

    /**
     * 已完成轮次。
     */
    private Long completedCount;

    /**
     * 已正式刷库轮次。
     */
    private Long flushedCount;

    /**
     * 当前 segment 开始轮次（含）。
     */
    private Long currentSegmentStart;

    /**
     * 当前 segment 结束轮次（含）。
     */
    private Long currentSegmentEnd;

    /**
     * segment 大小。
     */
    private Integer segmentSize;

    /**
     * 启动时锁定的采集属性快照。
     */
    private GatherTaskStatSnapshot statSnapshot;

    /**
     * 缓存最近更新时间。
     */
    private LocalDateTime updatedAt;

    /**
     * 判断当前热态是否还有未正式刷库的已完成轮次。
     *
     * @return true-有，false-无
     */
    public boolean hasPendingRounds() {
        if (completedCount == null || flushedCount == null) {
            return false;
        }
        return completedCount.longValue() > flushedCount.longValue();
    }
}