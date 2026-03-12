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
 * 1. 这里只编排“立即开始 / 加入队列 / 手动停止”。
 * 2. 当前已经接入“替换旧采集任务前，先做推进 + flush + stop”的最小闭环。
 * 3. 当前仍然不负责查询接口、Controller、前端、完整队列自动消费。
 */
public interface GatherTaskAppService {

    /**
     * 立即开始采集。
     *
     * 语义：
     * 1. 当前没有运行中任务时，直接启动。
     * 2. 当前已有运行中任务时，先处理旧任务，再启动新任务。
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
     * 手动停止当前运行中的采集任务。
     *
     * 语义：
     * 1. 先推进到当前时刻。
     * 2. 再 flush pending_reward_pool 到正式库存。
     * 3. 再把任务状态更新为 STOPPED。
     *
     * @param command 停止命令
     * @return 停止结果
     */
    StopGatherTaskResult stopCurrent(StopGatherTaskCommand command);
}