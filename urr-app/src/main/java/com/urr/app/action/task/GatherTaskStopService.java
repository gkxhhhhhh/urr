package com.urr.app.action.task;

import com.urr.app.action.task.command.AdvanceGatherTaskCommand;
import com.urr.app.action.task.result.PendingRewardFlushResult;
import com.urr.app.action.task.result.StopGatherTaskResult;
import com.urr.domain.action.task.ActionTaskStatusEnum;
import com.urr.domain.action.task.ActionTaskStopReasonEnum;
import com.urr.domain.action.task.PlayerGatherTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 采集任务停止服务。
 *
 * 说明：
 * 1. 这里承接 stop / replace 共用的最小闭环。
 * 2. 顺序固定为：先 advance，再 flush，再 stop，再清 Redis 热态。
 * 3. 本次在 stop 收口后，补“尝试自动拉起队列下一条”的最小闭环。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GatherTaskStopService {

    /**
     * 采集任务 DB 仓储。
     */
    private final PlayerGatherTaskRepository playerGatherTaskRepository;

    /**
     * 采集任务 Redis 热态仓储。
     */
    private final PlayerGatherTaskRedisRepository playerGatherTaskRedisRepository;

    /**
     * 采集任务懒推进服务。
     */
    private final GatherTaskAdvanceService gatherTaskAdvanceService;

    /**
     * 采集任务 pending flush 服务。
     */
    private final GatherTaskPendingRewardFlushService gatherTaskPendingRewardFlushService;

    /**
     * 队列自动拉起服务。
     */
    private final GatherTaskQueueAutoStartService gatherTaskQueueAutoStartService;

    /**
     * 停止一条采集任务。
     *
     * @param taskId 任务ID
     * @param stopReason 停止原因
     * @param stopTime 停止操作时间
     * @return 停止结果
     */
    @Transactional(rollbackFor = Exception.class)
    public StopGatherTaskResult stopByTaskId(Long taskId,
                                             ActionTaskStopReasonEnum stopReason,
                                             LocalDateTime stopTime) {
        validateInput(taskId, stopReason);

        LocalDateTime operateTime = stopTime == null ? LocalDateTime.now() : stopTime;
        PlayerGatherTask task = requireRunningTask(taskId);

        advanceToCurrentTime(task.getId(), operateTime);
        PendingRewardFlushResult flushResult = gatherTaskPendingRewardFlushService.flushPendingReward(task.getId());

        PlayerGatherTask latestTask = playerGatherTaskRepository.findByTaskId(task.getId());
        if (latestTask == null) {
            throw new IllegalStateException("停止采集任务后重新读取任务失败，taskId=" + task.getId());
        }

        latestTask.setStatus(ActionTaskStatusEnum.STOPPED);
        latestTask.setStopReason(stopReason);
        latestTask.setLastInteractTime(operateTime);
        playerGatherTaskRepository.update(latestTask);

        playerGatherTaskRedisRepository.deleteTaskHotState(latestTask.getPlayerId(), latestTask.getId());
        safeTryStartNextQueuedTask(latestTask.getPlayerId(), stopReason);

        return buildResult(latestTask, flushResult, operateTime);
    }

    /**
     * 在 stop 前先推进到当前时刻。
     *
     * @param taskId 任务ID
     * @param operateTime 操作时间
     */
    private void advanceToCurrentTime(Long taskId, LocalDateTime operateTime) {
        AdvanceGatherTaskCommand command = new AdvanceGatherTaskCommand();
        command.setTaskId(taskId);
        command.setAdvanceTime(operateTime);
        gatherTaskAdvanceService.advanceTo(command);
    }

    /**
     * 查询并校验当前任务必须是运行中的采集任务。
     *
     * @param taskId 任务ID
     * @return 运行中的采集任务
     */
    private PlayerGatherTask requireRunningTask(Long taskId) {
        PlayerGatherTask task = playerGatherTaskRepository.findByTaskId(taskId);
        if (task == null) {
            throw new IllegalArgumentException("采集任务不存在，taskId=" + taskId);
        }
        if (!task.isRunning()) {
            throw new IllegalStateException("当前采集任务不是运行中状态，taskId=" + taskId);
        }
        return task;
    }

    /**
     * 组装停止结果。
     *
     * @param task 最新任务状态
     * @param flushResult flush 结果
     * @param stopTime 停止时间
     * @return 停止结果
     */
    private StopGatherTaskResult buildResult(PlayerGatherTask task,
                                             PendingRewardFlushResult flushResult,
                                             LocalDateTime stopTime) {
        StopGatherTaskResult result = new StopGatherTaskResult();
        result.setTaskId(task.getId());
        result.setPlayerId(task.getPlayerId());
        result.setStatus(task.getStatus() == null ? null : task.getStatus().getCode());
        result.setStopReason(task.getStopReason() == null ? null : task.getStopReason().getCode());
        result.setCompletedCount(task.getSafeCompletedCount());
        result.setFlushedCount(task.getSafeFlushedCount());
        result.setFlushedRoundCount(flushResult == null ? 0L : flushResult.getFlushedRoundCount());
        result.setAppliedRewardEntryCount(flushResult == null ? 0 : flushResult.getAppliedRewardEntryCount());
        result.setRewardFlushed(flushResult != null && Boolean.TRUE.equals(flushResult.getRewardFlushed()));
        result.setStopTime(stopTime);
        return result;
    }

    /**
     * 校验入参。
     *
     * @param taskId 任务ID
     * @param stopReason 停止原因
     */
    private void validateInput(Long taskId, ActionTaskStopReasonEnum stopReason) {
        if (taskId == null) {
            throw new IllegalArgumentException("taskId不能为空");
        }
        if (stopReason == null) {
            throw new IllegalArgumentException("stopReason不能为空");
        }
    }

    /**
     * 安全尝试自动拉起下一条队列任务。
     *
     * 说明：
     * 1. USER_REPLACE 不自动消费队列，因为替换路径本身会立即启动新任务。
     * 2. 自动拉起失败时，不影响本次 stop 已经成功。
     *
     * @param playerId 玩家ID
     * @param stopReason 停止原因
     */
    private void safeTryStartNextQueuedTask(Long playerId, ActionTaskStopReasonEnum stopReason) {
        if (!shouldAutoStartNextTask(stopReason)) {
            return;
        }
        try {
            gatherTaskQueueAutoStartService.tryStartNextQueuedTask(playerId);
        } catch (Exception e) {
            log.warn("停止采集任务后自动拉起下一队列任务失败，playerId={}, stopReason={}", playerId, stopReason, e);
        }
    }

    /**
     * 判断当前停止原因是否允许自动拉起下一条任务。
     *
     * @param stopReason 停止原因
     * @return true-允许自动拉起，false-不允许
     */
    private boolean shouldAutoStartNextTask(ActionTaskStopReasonEnum stopReason) {
        return !ActionTaskStopReasonEnum.USER_REPLACE.equals(stopReason);
    }
}