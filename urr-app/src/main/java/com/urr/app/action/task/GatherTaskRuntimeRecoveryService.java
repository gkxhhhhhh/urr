package com.urr.app.action.task;

import com.urr.domain.action.task.ActionTaskTypeEnum;
import com.urr.domain.action.task.PlayerActionTask;
import com.urr.domain.action.task.PlayerGatherTask;
import com.urr.domain.action.task.PlayerRunningTaskCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 采集任务运行态恢复服务。
 *
 * 说明：
 * 1. 这里只做“数据库真相层 -> Redis 热态层”的按需恢复。
 * 2. 不负责推进、不负责停止、不负责 flush。
 * 3. 只恢复当前运行中的采集任务，不处理队列自动消费。
 */
@Service
@RequiredArgsConstructor
public class GatherTaskRuntimeRecoveryService {

    /**
     * 动作任务根表仓储。
     */
    private final PlayerActionTaskRepository playerActionTaskRepository;

    /**
     * 采集任务数据库真相层仓储。
     */
    private final PlayerGatherTaskRepository playerGatherTaskRepository;

    /**
     * 采集任务 Redis 热态仓储。
     */
    private final PlayerGatherTaskRedisRepository playerGatherTaskRedisRepository;

    /**
     * 按玩家ID恢复当前运行中的采集任务热态。
     *
     * @param playerId 玩家ID
     * @return 当前运行中的采集任务，不存在时返回 null
     */
    public PlayerGatherTask recoverRunningTaskByPlayerId(Long playerId) {
        if (playerId == null) {
            return null;
        }

        PlayerActionTask runningTask = playerActionTaskRepository.findRunningByPlayerId(playerId);
        if (runningTask == null) {
            return null;
        }
        if (!ActionTaskTypeEnum.GATHER.equals(runningTask.getTaskType())) {
            return null;
        }

        return recoverRunningTaskByTaskId(runningTask.getId());
    }

    /**
     * 按任务ID恢复当前运行中的采集任务热态。
     *
     * @param taskId 任务ID
     * @return 当前运行中的采集任务，不存在时返回 null
     */
    public PlayerGatherTask recoverRunningTaskByTaskId(Long taskId) {
        if (taskId == null) {
            return null;
        }

        PlayerGatherTask task = playerGatherTaskRepository.findByTaskId(taskId);
        if (task == null) {
            return null;
        }

        recoverHotStateIfNecessary(task);
        return task;
    }

    /**
     * 当 Redis 热态缺失时，按数据库真相层恢复当前任务热态。
     *
     * @param task 采集任务
     * @return true-执行了恢复，false-无需恢复
     */
    public boolean recoverHotStateIfNecessary(PlayerGatherTask task) {
        if (!canRecover(task)) {
            return false;
        }

        if (!isRunningSlotMissing(task)
                && !isRuntimeMissing(task)
                && !isSegmentMissing(task)
                && !isPendingRewardPoolMissing(task)) {
            return false;
        }

        playerGatherTaskRedisRepository.saveTaskHotState(task);
        return true;
    }

    /**
     * 判断当前任务是否满足恢复条件。
     *
     * @param task 采集任务
     * @return true-可恢复，false-不可恢复
     */
    private boolean canRecover(PlayerGatherTask task) {
        if (task == null) {
            return false;
        }
        if (task.getId() == null) {
            return false;
        }
        if (task.getPlayerId() == null) {
            return false;
        }
        return task.isRunning();
    }

    /**
     * 判断当前运行槽位是否缺失。
     *
     * @param task 采集任务
     * @return true-缺失，false-存在
     */
    private boolean isRunningSlotMissing(PlayerGatherTask task) {
        PlayerRunningTaskCache cache = playerGatherTaskRedisRepository.findRunningTaskByPlayerId(task.getPlayerId());
        if (cache == null) {
            return true;
        }
        if (!task.getId().equals(cache.getTaskId())) {
            return true;
        }
        return !cache.isGatherTask();
    }

    /**
     * 判断采集运行态是否缺失。
     *
     * @param task 采集任务
     * @return true-缺失，false-存在
     */
    private boolean isRuntimeMissing(PlayerGatherTask task) {
        return playerGatherTaskRedisRepository.findRuntimeByTaskId(task.getId()) == null;
    }

    /**
     * 判断当前 segment plan 是否缺失。
     *
     * @param task 采集任务
     * @return true-缺失，false-存在
     */
    private boolean isSegmentMissing(PlayerGatherTask task) {
        boolean shouldHaveSegment =
                (task.getCurrentSegmentStart() != null && task.getCurrentSegmentEnd() != null)
                        || task.hasLockedRewardPlan();

        if (!shouldHaveSegment) {
            return false;
        }

        return playerGatherTaskRedisRepository.findSegmentPlanByTaskId(task.getId()) == null;
    }

    /**
     * 判断 pending_reward_pool 是否缺失。
     *
     * @param task 采集任务
     * @return true-缺失，false-存在
     */
    private boolean isPendingRewardPoolMissing(PlayerGatherTask task) {
        boolean shouldHavePending = task.hasPendingRewardPool() || task.hasPendingRewardToFlush();
        if (!shouldHavePending) {
            return false;
        }

        return playerGatherTaskRedisRepository.findPendingRewardPoolByTaskId(task.getId()) == null;
    }
}