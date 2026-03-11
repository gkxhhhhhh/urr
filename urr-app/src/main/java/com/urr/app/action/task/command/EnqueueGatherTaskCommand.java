package com.urr.app.action.task.command;

import lombok.Data;

/**
 * 加入采集队列命令。
 */
@Data
public class EnqueueGatherTaskCommand {

    /**
     * 账号ID。
     */
    private Long accountId;

    /**
     * 玩家ID。
     */
    private Long playerId;

    /**
     * 动作编码。
     */
    private String actionCode;

    /**
     * 目标轮次。
     * -1 表示无限次数。
     */
    private Long targetCount;
}