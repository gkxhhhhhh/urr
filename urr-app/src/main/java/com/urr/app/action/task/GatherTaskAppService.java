package com.urr.app.action.task;

import com.urr.app.action.task.command.EnqueueGatherTaskCommand;
import com.urr.app.action.task.command.StartGatherTaskCommand;
import com.urr.app.action.task.result.StartGatherTaskResult;

/**
 * 采集任务应用服务。
 *
 * 说明：
 * 1. 这里只编排“立即开始 / 加入队列 / 替换当前任务”。
 * 2. 不负责懒结算、不负责 stop/flush 完整逻辑、不负责队列自动消费。
 * 3. Controller 后续只需要直接调用这里即可。
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
}