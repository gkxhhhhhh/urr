package com.urr.app.action.task;

import com.urr.app.action.task.command.EnqueueGatherTaskCommand;
import com.urr.app.action.task.command.StartGatherTaskCommand;
import com.urr.app.action.task.command.StopGatherTaskCommand;
import com.urr.app.action.task.result.StartGatherTaskResult;
import com.urr.app.action.task.result.StopGatherTaskResult;

/**
 * 制造任务应用服务。
 */
public interface CraftTaskAppService {

    /**
     * 立即开始制造。
     *
     * @param command 开始命令
     * @return 启动结果
     */
    StartGatherTaskResult startNow(StartGatherTaskCommand command);

    /**
     * 制造入队。
     *
     * @param command 入队命令
     * @return 结果
     */
    StartGatherTaskResult enqueue(EnqueueGatherTaskCommand command);

    /**
     * 停止当前制造任务。
     *
     * @param command 停止命令
     * @return 停止结果
     */
    StopGatherTaskResult stopCurrent(StopGatherTaskCommand command);
}