package com.urr.app.action.task;

import com.urr.domain.action.task.ActionTaskTypeEnum;
import com.urr.domain.action.task.PlayerActionTask;
import com.urr.domain.action.task.PlayerGatherTask;
import com.urr.domain.action.task.PlayerRunningTaskCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 采集任务运行态恢复服务。
 *
 * 说明：
 * 1. 这里只负责 DB -> Redis 热态恢复，以及“无当前运行任务时的队列兜底拉起”。
 * 2. 不负责 advance / flush / stop。
 * 3. 当前只处理采集任务，不扩散到其他任务类型。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GatherTaskRuntimeRecoveryService {

    /**
     * 动作根任务仓储。
     */
    private final PlayerActionTaskRepository playerActionTaskRepository;

    /**
     * 采集任务仓储。
     */
    private final PlayerGatherTaskRepository playerGatherTaskRepository;

    /**
     * 采集任务 Redis 热态仓储。
     */
    private final PlayerGatherTaskRedisRepository playerGatherTaskRedisRepository;

    /**
     * 队列自动拉起服务。
     */
    private final GatherTaskQueueAutoStartService gatherTaskQueueAutoStartService;

    /**
     * 按玩家ID恢复当前运行中的采集任务。
     *
     * 说明：
     * 1. 如果 DB 里有运行中的采集任务，则恢复其 Redis 热态并返回。
     * 2. 如果 DB 里没有运行中的任务，但队列里有待运行采集任务，则尝试兜底拉起队头任务。
     *
     * @param playerId 玩家ID
     * @return 当前运行中的采集任务；不存在时返回 null
     */
    public PlayerGatherTask recoverRunningTaskByPlayerId(Long playerId) {
        if (playerId == null) {
            return null;
        }

        PlayerActionTask runningTask = playerActionTaskRepository.findRunningByPlayerId(playerId);
        if (runningTask == null) {
            return safeTryStartNextQueuedTask(playerId);
        }
        if (!ActionTaskTypeEnum.GATHER.equals(runningTask.getTaskType())) {
            return null;
        }

        PlayerGatherTask gatherTask = playerGatherTaskRepository.findByTaskId(runningTask.getId());
        if (gatherTask == null) {
            return null;
        }

        recoverHotStateIfNecessary(gatherTask);
        return gatherTask;
    }

    /**
     * 在 Redis 热态缺失时，按任务镜像最小恢复热态。
     *
     * @param task 采集任务
     */
    public void recoverHotStateIfNecessary(PlayerGatherTask task) {
        if (task == null) {
            return;
        }
        if (!task.isRunning()) {
            return;
        }
        if (!shouldRecoverHotState(task)) {
            return;
        }
        playerGatherTaskRedisRepository.saveTaskHotState(task);
    }

    /**
     * 判断当前任务是否需要恢复 Redis 热态。
     *
     * @param task 采集任务
     * @return true-需要恢复，false-无需恢复
     */
    private boolean shouldRecoverHotState(PlayerGatherTask task) {
        PlayerRunningTaskCache runningTaskCache =
                playerGatherTaskRedisRepository.findRunningTaskByPlayerId(task.getPlayerId());
        if (runningTaskCache == null) {
            return true;
        }
        if (!task.getId().equals(runningTaskCache.getTaskId())) {
            return true;
        }
        if (!runningTaskCache.isGatherTask()) {
            return true;
        }
        if (playerGatherTaskRedisRepository.findRuntimeByTaskId(task.getId()) == null) {
            return true;
        }
        if (task.hasLockedRewardPlan()
                && playerGatherTaskRedisRepository.findSegmentPlanByTaskId(task.getId()) == null) {
            return true;
        }
        if ((task.hasPendingRewardPool() || task.hasPendingRewardToFlush())
                && playerGatherTaskRedisRepository.findPendingRewardPoolByTaskId(task.getId()) == null) {
            return true;
        }
        return false;
    }

    /**
     * 安全尝试拉起下一条队列任务。
     *
     * @param playerId 玩家ID
     * @return 新拉起的采集任务；没有拉起时返回 null
     */
    private PlayerGatherTask safeTryStartNextQueuedTask(Long playerId) {
        try {
            return gatherTaskQueueAutoStartService.tryStartNextQueuedTask(playerId);
        } catch (Exception e) {
            log.warn("恢复场景下自动拉起下一队列任务失败，playerId={}", playerId, e);
            return null;
        }
    }
}