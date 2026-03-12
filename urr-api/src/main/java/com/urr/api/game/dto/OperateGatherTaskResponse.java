package com.urr.api.game.dto;

import lombok.Data;

/**
 * 采集写接口响应。
 */
@Data
public class OperateGatherTaskResponse {

    /**
     * 本次操作模式。
     */
    private String mode;

    /**
     * 角色ID。
     */
    private Long playerId;

    /**
     * 动作编码。
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
     * 直接启动时有值，入队时可能为空。
     */
    private Long taskId;

    /**
     * 队列ID。
     * 入队时有值，直接启动时为空。
     */
    private Long queueId;

    /**
     * 当前状态。
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
}