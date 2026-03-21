package com.urr.app.action.task.command;

import lombok.Data;

/**
 * 职业动作命令。
 *
 * 说明：
 * 1. 这是 app 层命令对象，不直接承载 API 语义。
 * 2. 当前只收口到已经真实落地的 gather task 能力。
 */
@Data
public class ProfessionActionCommand {

    /**
     * 账号ID。
     */
    private Long accountId;

    /**
     * 角色ID。
     */
    private Long playerId;

    /**
     * 命令类型。
     * START_NOW / ENQUEUE / STOP / REFRESH
     */
    private String commandType;

    /**
     * 动作编码。
     */
    private String actionCode;

    /**
     * 目标次数。
     */
    private Long targetCount;
}