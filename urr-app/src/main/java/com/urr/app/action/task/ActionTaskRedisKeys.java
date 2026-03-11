package com.urr.app.action.task;

import com.urr.domain.action.task.ActionTaskConstants;

import java.time.Duration;

/**
 * 动作任务 Redis key 与 TTL 常量。
 *
 * 说明：
 * 1. 本类只定义热态 key，不承载数据库真相语义。
 * 2. key 尽量保持直白、可读、可按 playerId / taskId 快速定位。
 * 3. TTL 不做太短，避免任务仍然有效时热态先过期。
 */
public final class ActionTaskRedisKeys {

    /**
     * Redis key 总前缀。
     */
    public static final String PREFIX = "urr";

    /**
     * 玩家当前运行槽位 TTL。
     * 24 小时离线上限外，再额外留 12 小时缓冲。
     */
    public static final Duration PLAYER_RUNNING_TASK_TTL = Duration.ofHours(ActionTaskConstants.MAX_OFFLINE_HOURS + 12L);

    /**
     * 采集任务运行态 TTL。
     */
    public static final Duration GATHER_RUNTIME_TTL = Duration.ofHours(ActionTaskConstants.MAX_OFFLINE_HOURS + 12L);

    /**
     * 当前 segment plan TTL。
     */
    public static final Duration GATHER_SEGMENT_PLAN_TTL = Duration.ofHours(ActionTaskConstants.MAX_OFFLINE_HOURS + 12L);

    /**
     * pending_reward_pool TTL。
     * 比运行态多留一些缓冲时间，方便后续先 flush 再消费。
     */
    public static final Duration GATHER_PENDING_REWARD_POOL_TTL = Duration.ofHours(ActionTaskConstants.MAX_OFFLINE_HOURS + 24L);

    /**
     * 工具类不允许实例化。
     */
    private ActionTaskRedisKeys() {
    }

    /**
     * 构建玩家当前运行动作槽位 key。
     *
     * @param playerId 玩家ID
     * @return Redis key
     */
    public static String buildPlayerRunningTaskKey(Long playerId) {
        validateId("playerId", playerId);
        return buildKey(PREFIX, "player", "action", "running", String.valueOf(playerId));
    }

    /**
     * 构建采集任务运行态 key。
     *
     * @param taskId 任务ID
     * @return Redis key
     */
    public static String buildGatherRuntimeKey(Long taskId) {
        validateId("taskId", taskId);
        return buildKey(PREFIX, "gather", "task", "runtime", String.valueOf(taskId));
    }

    /**
     * 构建采集任务当前 segment plan key。
     *
     * @param taskId 任务ID
     * @return Redis key
     */
    public static String buildGatherSegmentPlanKey(Long taskId) {
        validateId("taskId", taskId);
        return buildKey(PREFIX, "gather", "task", "segment-plan", String.valueOf(taskId));
    }

    /**
     * 构建采集任务 pending_reward_pool key。
     *
     * @param taskId 任务ID
     * @return Redis key
     */
    public static String buildGatherPendingRewardPoolKey(Long taskId) {
        validateId("taskId", taskId);
        return buildKey(PREFIX, "gather", "task", "pending-reward-pool", String.valueOf(taskId));
    }

    /**
     * 拼装 Redis key。
     *
     * @param parts key 片段
     * @return Redis key
     */
    private static String buildKey(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append(':');
            }
            builder.append(parts[i]);
        }
        return builder.toString();
    }

    /**
     * 校验 ID 参数。
     *
     * @param fieldName 字段名
     * @param value 字段值
     */
    private static void validateId(String fieldName, Long value) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
    }
}