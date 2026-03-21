package com.urr.app.action.task.result;

import lombok.Data;

/**
 * 职业动作命令结果。
 *
 * 说明：
 * 1. 统一承接命令执行结果和最新面板结果。
 * 2. operateResult 对应 START_NOW / ENQUEUE。
 * 3. stopResult 对应 STOP。
 * 4. latestPanel 对应命令执行后的最新状态摘要。
 */
@Data
public class ProfessionActionCommandResult {

    /**
     * 本次命令类型。
     */
    private String commandType;

    /**
     * 是否成功。
     */
    private Boolean success;

    /**
     * 提示信息。
     */
    private String message;

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
     * 启动 / 入队结果。
     */
    private StartGatherTaskResult operateResult;

    /**
     * 停止结果。
     */
    private StopGatherTaskResult stopResult;

    /**
     * 最新面板结果。
     */
    private QueryGatherTaskPanelResult latestPanel;
}