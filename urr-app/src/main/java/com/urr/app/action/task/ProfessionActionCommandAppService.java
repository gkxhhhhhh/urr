package com.urr.app.action.task;

import com.urr.app.action.task.command.ProfessionActionCommand;
import com.urr.app.action.task.result.ProfessionActionCommandResult;

/**
 * 职业动作命令应用服务。
 *
 * 说明：
 * 1. 统一收口职业动作命令入口。
 * 2. 当前内部仍只复用现有 gather task 能力，不做通用行为树重构。
 */
public interface ProfessionActionCommandAppService {

    /**
     * 执行职业动作命令。
     *
     * @param command 职业动作命令
     * @return 命令结果
     */
    ProfessionActionCommandResult execute(ProfessionActionCommand command);
}