package com.urr.app.action.task.result;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 手动停止采集任务结果。
 *
 * 说明：
 * 1. 这里只返回 stop / flush 闭环后的最小必要字段。
 * 2. 当前不承载查询接口展示模型。
 */
@Data
public class StopGatherTaskResult {

    /**
     * 任务ID。
     */
    private Long taskId;

    /**
     * 玩家ID。
     */
    private Long playerId;

    /**
     * 停止后的任务状态。
     */
    private String status;

    /**
     * 停止原因。
     */
    private String stopReason;

    /**
     * 当前已完成轮次。
     */
    private Long completedCount;

    /**
     * 当前已正式入库轮次。
     */
    private Long flushedCount;

    /**
     * 本次 flush 的轮次数。
     */
    private Long flushedRoundCount;

    /**
     * 本次正式入库的奖励条目数。
     */
    private Integer appliedRewardEntryCount;

    /**
     * 本次是否真的发生了正式库存写入。
     */
    private Boolean rewardFlushed;

    /**
     * 本次停止操作时间。
     */
    private LocalDateTime stopTime;
}