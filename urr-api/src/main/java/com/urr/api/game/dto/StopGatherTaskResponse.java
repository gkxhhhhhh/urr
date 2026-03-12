package com.urr.api.game.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 停止采集任务响应。
 */
@Data
public class StopGatherTaskResponse {

    /**
     * 是否成功。
     */
    private Boolean success;

    /**
     * 任务ID。
     */
    private Long taskId;

    /**
     * 角色ID。
     */
    private Long playerId;

    /**
     * 停止后的状态。
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
     * 停止时间。
     */
    private LocalDateTime stopTime;

    /**
     * 本次 flush 的最小信息。
     */
    private FlushInfo flush;

    /**
     * flush 最小结果。
     */
    @Data
    public static class FlushInfo {

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
    }
}