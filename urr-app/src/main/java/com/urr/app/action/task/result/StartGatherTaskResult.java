package com.urr.app.action.task.result;

import lombok.Data;

/**
 * 采集任务启动结果。
 *
 * 说明：
 * 1. 既承接“立即启动”的结果，也承接“成功入队”的结果。
 * 2. 这里故意只返回最小必要字段，避免这轮把查询模型也一起做重。
 */
@Data
public class StartGatherTaskResult {

    /**
     * 任务ID。
     * 直接启动时，这里返回真实 taskId。
     * 入队时，这里为空。
     */
    private Long taskId;

    /**
     * 队列记录ID。
     * 入队时返回，直接启动时为空。
     */
    private Long queueId;

    /**
     * 玩家ID。
     */
    private Long playerId;

    /**
     * 动作编码。
     */
    private String actionCode;

    /**
     * 当前结果状态。
     * 直接启动时一般为 RUNNING，入队时为 QUEUED。
     */
    private String status;

    /**
     * 是否进入了队列。
     */
    private Boolean queued;

    /**
     * 当前排队位置。
     * 直接启动时为空。
     */
    private Integer queuePosition;

    /**
     * 被替换掉的旧任务ID。
     * 没有替换时为空。
     */
    private Long replacedTaskId;
}