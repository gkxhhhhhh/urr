package com.urr.app.action.task.result;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推进采集任务结果。
 */
@Data
public class AdvanceGatherTaskResult {

    /**
     * 任务ID。
     */
    private Long taskId;

    /**
     * 推进请求时间。
     */
    private LocalDateTime requestTime;

    /**
     * 实际生效时间。
     */
    private LocalDateTime effectiveNow;

    /**
     * 推进前已完成轮次。
     */
    private Long beforeCompletedCount;

    /**
     * 推进后已完成轮次。
     */
    private Long afterCompletedCount;

    /**
     * 本次新增完成轮次。
     */
    private Long advancedRoundCount;

    /**
     * 推进前最近一次任务层结算时间。
     */
    private LocalDateTime beforeLastSettleTime;

    /**
     * 推进后最近一次任务层结算时间。
     */
    private LocalDateTime afterLastSettleTime;

    /**
     * 当前段开始轮次。
     */
    private Long currentSegmentStart;

    /**
     * 当前段结束轮次。
     */
    private Long currentSegmentEnd;

    /**
     * 是否写回了数据库真相层。
     */
    private Boolean dbUpdated;

    /**
     * 是否刷新了 Redis 热态层。
     */
    private Boolean redisUpdated;
}