package com.urr.app.action.task.command;

import lombok.Data;

/**
 * 手动停止当前采集任务命令。
 */
@Data
public class StopGatherTaskCommand {

    /**
     * 账号ID。
     */
    private Long accountId;

    /**
     * 玩家ID。
     */
    private Long playerId;
}