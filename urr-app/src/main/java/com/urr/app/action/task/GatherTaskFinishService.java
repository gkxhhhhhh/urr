package com.urr.app.action.task;

import com.urr.domain.action.task.ActionTaskStatusEnum;
import com.urr.domain.action.task.ActionTaskStopReasonEnum;
import com.urr.domain.action.task.PlayerGatherTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 采集任务完成收口服务。
 *
 * 说明：
 * 1. 这里只负责“达到目标轮次后的完成态收口”。
 * 2. 顺序固定为：先 flush，再标记 COMPLETED，再清旧热态，再尝试拉起队列下一条。
 * 3. 不负责普通 stop 场景。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GatherTaskFinishService {

    /**
     * 采集任务仓储。
     */
    private final PlayerGatherTaskRepository playerGatherTaskRepository;

    /**
     * 采集任务 Redis 热态仓储。
     */
    private final PlayerGatherTaskRedisRepository playerGatherTaskRedisRepository;

    /**
     * pending_reward_pool flush 服务。
     */
    private final GatherTaskPendingRewardFlushService gatherTaskPendingRewardFlushService;

    /**
     * 队列自动拉起服务。
     */
    private final GatherTaskQueueAutoStartService gatherTaskQueueAutoStartService;

    /**
     * 当采集任务达到目标轮次时，执行完成态收口。
     *
     * @param taskId 任务ID
     * @param finishTime 完成时间
     * @return 最新任务状态
     */
    @Transactional(rollbackFor = Exception.class)
    public PlayerGatherTask finishIfReachedTarget(Long taskId, LocalDateTime finishTime) {
        if (taskId == null) {
            return null;
        }

        LocalDateTime operateTime = finishTime == null ? LocalDateTime.now() : finishTime;
        PlayerGatherTask task = playerGatherTaskRepository.findByTaskId(taskId);
        if (task == null) {
            return null;
        }
        if (!task.isRunning()) {
            return task;
        }
        if (!task.isFinishedByTargetCount()) {
            return task;
        }

        gatherTaskPendingRewardFlushService.flushPendingReward(task.getId());

        PlayerGatherTask latestTask = playerGatherTaskRepository.findByTaskId(taskId);
        if (latestTask == null) {
            throw new IllegalStateException("完成采集任务后重新读取失败，taskId=" + taskId);
        }
        if (!latestTask.isRunning()) {
            return latestTask;
        }

        latestTask.setStatus(ActionTaskStatusEnum.COMPLETED);
        latestTask.setStopReason(ActionTaskStopReasonEnum.FINISHED);
        latestTask.setLastInteractTime(operateTime);
        playerGatherTaskRepository.update(latestTask);

        playerGatherTaskRedisRepository.deleteTaskHotState(latestTask.getPlayerId(), latestTask.getId());
        safeTryStartNextQueuedTask(latestTask.getPlayerId());

        return playerGatherTaskRepository.findByTaskId(taskId);
    }

    /**
     * 尝试安全拉起下一条排队任务。
     *
     * 说明：
     * 1. 下一条自动拉起失败时，不影响当前任务已经完成的事实。
     * 2. 这里只做兜底日志，不把异常继续向外抛。
     *
     * @param playerId 玩家ID
     */
    private void safeTryStartNextQueuedTask(Long playerId) {
        try {
            gatherTaskQueueAutoStartService.tryStartNextQueuedTask(playerId);
        } catch (Exception e) {
            log.warn("采集任务完成后自动拉起下一队列任务失败，playerId={}", playerId, e);
        }
    }
}