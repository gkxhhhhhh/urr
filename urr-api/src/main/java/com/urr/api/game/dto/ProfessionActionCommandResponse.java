package com.urr.api.game.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 职业动作命令响应。
 *
 * 说明：
 * 1. 顶层返回本次命令结果。
 * 2. latestPanel 直接复用当前采集面板响应结构，方便前端无缝接入。
 * 3. 这轮不额外发明前端当前未真实消费的大量摘要 DTO。
 */
@Data
public class ProfessionActionCommandResponse {

    /**
     * 本次命令类型。
     */
    private String commandType;

    /**
     * 是否成功。
     */
    private Boolean success;

    /**
     * 本次命令结果提示。
     */
    private String message;

    /**
     * 角色ID。
     */
    private Long playerId;

    /**
     * 动作编码。
     * START_NOW / ENQUEUE 时通常有值。
     */
    private String actionCode;

    /**
     * 目标次数。
     */
    private Long targetCount;

    /**
     * 是否无限次数。
     */
    private Boolean infiniteTarget;

    /**
     * 任务ID。
     * 立即开始 / 停止时通常有值。
     */
    private Long taskId;

    /**
     * 队列ID。
     * 入队时通常有值。
     */
    private Long queueId;

    /**
     * 当前结果状态。
     */
    private String status;

    /**
     * 是否进入了队列。
     */
    private Boolean queued;

    /**
     * 当前队列位置。
     */
    private Integer queuePosition;

    /**
     * 被替换的旧任务ID。
     */
    private Long replacedTaskId;

    /**
     * 停止原因。
     * STOP 命令时可能有值。
     */
    private String stopReason;

    /**
     * 当前已完成轮次。
     * STOP 命令时可能有值。
     */
    private Long completedCount;

    /**
     * 当前已正式入库轮次。
     * STOP 命令时可能有值。
     */
    private Long flushedCount;

    /**
     * 停止时间。
     * STOP 命令时可能有值。
     */
    private LocalDateTime stopTime;

    /**
     * 本次 flush 最小摘要。
     */
    private FlushInfo flush;

    /**
     * 命令执行后的最新面板。
     */
    private GatherTaskPanelResponse latestPanel;

    /**
     * flush 最小信息。
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