package com.urr.app.action.task;

import com.urr.app.action.task.command.EnqueueGatherTaskCommand;
import com.urr.app.action.task.command.StartGatherTaskCommand;
import com.urr.app.action.task.command.StopGatherTaskCommand;
import com.urr.app.action.task.result.StartGatherTaskResult;
import com.urr.app.action.task.result.StopGatherTaskResult;

/**
 * 采集任务应用服务。
 *
 * 说明：
 * 1. 这里只编排“立即开始 / 加入队列 / 停止当前任务”的最小写能力。
 * 2. 不负责懒结算细节，不负责完整恢复，不负责自动消费队列。
 * 3. Controller 只需要调用这里即可。
 */
public interface GatherTaskAppService {

    /**
     * 立即开始采集。
     *
     * 语义：
     * 1. 当前没有运行中任务时，直接启动。
     * 2. 当前已有运行中任务时，替换当前任务并启动新的采集任务。
     *
     * @param command 开始采集命令
     * @return 启动结果
     */
    StartGatherTaskResult startNow(StartGatherTaskCommand command);

    /**
     * 加入采集队列。
     *
     * 语义：
     * 1. 当前没有运行中任务时，直接启动。
     * 2. 当前已有运行中任务时，记录为排队状态。
     *
     * @param command 入队命令
     * @return 启动或入队结果
     */
    StartGatherTaskResult enqueue(EnqueueGatherTaskCommand command);

    /**
     * 停止当前运行中的采集任务。
     *
     * 语义：
     * 1. 这里按“账号 + 角色”定位当前运行中的任务。
     * 2. 只允许停止当前运行中的采集任务。
     * 3. 真正的 stop / flush 闭环仍复用现有停止服务。
     *
     * @param command 停止命令
     * @return 停止结果
     */
    StopGatherTaskResult stopCurrent(StopGatherTaskCommand command);
}