package com.urr.app.action.task;

import com.urr.domain.action.task.ActionTaskStatusEnum;
import com.urr.domain.action.task.ActionTaskStopReasonEnum;
import com.urr.domain.action.task.ActionTaskTypeEnum;
import com.urr.domain.action.task.GatherTaskPendingRewardPoolCache;
import com.urr.domain.action.task.GatherTaskSegmentPlanCache;
import com.urr.domain.action.task.PlayerGatherTask;
import com.urr.domain.action.task.PlayerGatherTaskRuntimeCache;
import com.urr.domain.action.task.PlayerRunningTaskCache;

import java.time.LocalDateTime;

/**
 * 采集任务与 Redis 热态对象之间的转换器。
 *
 * 说明：
 * 1. 这里只做对象转换，不做业务编排。
 * 2. Redis 是热态镜像，所以这里的转换方向主要是“数据库/领域对象 -> Redis 热态对象”。
 */
public final class PlayerGatherTaskRedisConverter {

    /**
     * 工具类不允许实例化。
     */
    private PlayerGatherTaskRedisConverter() {
    }

    /**
     * 把采集任务转换成玩家当前运行槽位缓存。
     *
     * @param task 采集任务
     * @return 运行槽位缓存
     */
    public static PlayerRunningTaskCache toRunningTaskCache(PlayerGatherTask task) {
        if (task == null) {
            return null;
        }

        PlayerRunningTaskCache cache = new PlayerRunningTaskCache();
        cache.setTaskId(task.getId());
        cache.setPlayerId(task.getPlayerId());
        cache.setServerId(task.getServerId());
        cache.setActionCode(task.getActionCode());
        cache.setTaskType(getTaskTypeCode(task.getTaskType()));
        cache.setStatus(getStatusCode(task.getStatus()));
        cache.setStartTime(task.getStartTime());
        cache.setLastInteractTime(task.getLastInteractTime());
        cache.setOfflineExpireAt(task.getOfflineExpireAt());
        cache.setUpdatedAt(LocalDateTime.now());
        return cache;
    }

    /**
     * 把采集任务转换成采集运行态缓存。
     *
     * @param task 采集任务
     * @return 运行态缓存
     */
    public static PlayerGatherTaskRuntimeCache toRuntimeCache(PlayerGatherTask task) {
        if (task == null) {
            return null;
        }

        PlayerGatherTaskRuntimeCache cache = new PlayerGatherTaskRuntimeCache();
        cache.setTaskId(task.getId());
        cache.setPlayerId(task.getPlayerId());
        cache.setServerId(task.getServerId());
        cache.setActionCode(task.getActionCode());
        cache.setTaskType(getTaskTypeCode(task.getTaskType()));
        cache.setStatus(getStatusCode(task.getStatus()));
        cache.setStopReason(getStopReasonCode(task.getStopReason()));
        cache.setStartTime(task.getStartTime());
        cache.setLastInteractTime(task.getLastInteractTime());
        cache.setLastSettleTime(task.getLastSettleTime());
        cache.setOfflineExpireAt(task.getOfflineExpireAt());
        cache.setRewardSeed(task.getRewardSeed());
        cache.setTargetCount(task.getTargetCount());
        cache.setCompletedCount(task.getCompletedCount());
        cache.setFlushedCount(task.getFlushedCount());
        cache.setCurrentSegmentStart(task.getCurrentSegmentStart());
        cache.setCurrentSegmentEnd(task.getCurrentSegmentEnd());
        cache.setSegmentSize(task.getSegmentSize());
        cache.setStatSnapshot(task.getStatSnapshot());
        cache.setUpdatedAt(LocalDateTime.now());
        return cache;
    }

    /**
     * 把采集任务转换成当前 segment plan 缓存。
     *
     * @param task 采集任务
     * @return segment plan 缓存
     */
    public static GatherTaskSegmentPlanCache toSegmentPlanCache(PlayerGatherTask task) {
        if (task == null) {
            return null;
        }

        GatherTaskSegmentPlanCache cache = new GatherTaskSegmentPlanCache();
        cache.setTaskId(task.getId());
        cache.setPlayerId(task.getPlayerId());
        cache.setServerId(task.getServerId());
        cache.setSegmentStart(task.getCurrentSegmentStart());
        cache.setSegmentEnd(task.getCurrentSegmentEnd());
        cache.setSegmentSize(task.getSegmentSize());
        cache.setRewardSeed(task.getRewardSeed());
        cache.setPlanJson(task.getCurrentSegmentRewardPlanJson());
        cache.setCreatedAt(LocalDateTime.now());
        cache.setUpdatedAt(LocalDateTime.now());
        return cache;
    }

    /**
     * 把采集任务转换成 pending_reward_pool 缓存。
     *
     * @param task 采集任务
     * @return pending_reward_pool 缓存
     */
    public static GatherTaskPendingRewardPoolCache toPendingRewardPoolCache(PlayerGatherTask task) {
        if (task == null) {
            return null;
        }

        GatherTaskPendingRewardPoolCache cache = new GatherTaskPendingRewardPoolCache();
        cache.setTaskId(task.getId());
        cache.setPlayerId(task.getPlayerId());
        cache.setServerId(task.getServerId());
        cache.setCompletedCount(task.getCompletedCount());
        cache.setFlushedCount(task.getFlushedCount());
        cache.setPendingRoundCount(calculatePendingRoundCount(task.getCompletedCount(), task.getFlushedCount()));
        cache.setRewardPoolJson(task.getPendingRewardPoolJson());
        cache.setUpdatedAt(LocalDateTime.now());
        return cache;
    }

    /**
     * 计算待刷库轮次数。
     *
     * @param completedCount 已完成轮次
     * @param flushedCount 已刷库轮次
     * @return 待刷库轮次数
     */
    private static Long calculatePendingRoundCount(Long completedCount, Long flushedCount) {
        long completed = completedCount == null ? 0L : completedCount.longValue();
        long flushed = flushedCount == null ? 0L : flushedCount.longValue();
        long pending = completed - flushed;
        if (pending < 0L) {
            return 0L;
        }
        return pending;
    }

    /**
     * 获取任务类型编码。
     *
     * @param taskType 任务类型枚举
     * @return 编码
     */
    private static String getTaskTypeCode(ActionTaskTypeEnum taskType) {
        if (taskType == null) {
            return null;
        }
        return taskType.getCode();
    }

    /**
     * 获取任务状态编码。
     *
     * @param status 状态枚举
     * @return 编码
     */
    private static String getStatusCode(ActionTaskStatusEnum status) {
        if (status == null) {
            return null;
        }
        return status.getCode();
    }

    /**
     * 获取停止原因编码。
     *
     * @param stopReason 停止原因枚举
     * @return 编码
     */
    private static String getStopReasonCode(ActionTaskStopReasonEnum stopReason) {
        if (stopReason == null) {
            return null;
        }
        return stopReason.getCode();
    }
}