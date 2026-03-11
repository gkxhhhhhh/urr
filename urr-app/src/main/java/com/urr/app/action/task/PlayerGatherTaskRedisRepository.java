package com.urr.app.action.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urr.domain.action.task.GatherTaskPendingRewardPoolCache;
import com.urr.domain.action.task.GatherTaskSegmentPlanCache;
import com.urr.domain.action.task.PlayerGatherTask;
import com.urr.domain.action.task.PlayerGatherTaskRuntimeCache;
import com.urr.domain.action.task.PlayerRunningTaskCache;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * 采集任务 Redis 热态仓储。
 *
 * 说明：
 * 1. 这里只负责 Redis 热态读写，不负责启动、执行、结算。
 * 2. Redis 只是热态缓存，不是唯一真相源。
 * 3. 数据库仍然是恢复依据；Redis 丢失后，可基于数据库任务记录重新写回热态。
 */
@Repository
@RequiredArgsConstructor
public class PlayerGatherTaskRedisRepository {

    /**
     * 字符串 Redis 模板。
     *
     * 说明：
     * 1. 当前仓库没有直接读到现成的复杂 Redis 序列化封装。
     * 2. 这里直接使用 StringRedisTemplate + ObjectMapper，保持简单。
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * JSON 编解码器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 按采集任务领域对象整体刷新 Redis 热态。
     *
     * @param task 采集任务
     */
    public void saveTaskHotState(PlayerGatherTask task) {
        validateTask(task);

        saveRunningTask(PlayerGatherTaskRedisConverter.toRunningTaskCache(task));
        saveRuntime(PlayerGatherTaskRedisConverter.toRuntimeCache(task));

        if (shouldCacheSegmentPlan(task)) {
            saveSegmentPlan(PlayerGatherTaskRedisConverter.toSegmentPlanCache(task));
        } else {
            deleteSegmentPlan(task.getId());
        }

        if (shouldCachePendingRewardPool(task)) {
            savePendingRewardPool(PlayerGatherTaskRedisConverter.toPendingRewardPoolCache(task));
        } else {
            deletePendingRewardPool(task.getId());
        }
    }

    /**
     * 按玩家ID读取当前运行槽位。
     *
     * @param playerId 玩家ID
     * @return 运行槽位缓存，不存在时返回 null
     */
    public PlayerRunningTaskCache findRunningTaskByPlayerId(Long playerId) {
        if (playerId == null) {
            return null;
        }
        String key = ActionTaskRedisKeys.buildPlayerRunningTaskKey(playerId);
        return readJson(key, PlayerRunningTaskCache.class);
    }

    /**
     * 按玩家ID读取当前采集运行态。
     *
     * @param playerId 玩家ID
     * @return 采集运行态缓存，不存在时返回 null
     */
    public PlayerGatherTaskRuntimeCache findCurrentGatherRuntimeByPlayerId(Long playerId) {
        PlayerRunningTaskCache runningTaskCache = findRunningTaskByPlayerId(playerId);
        if (runningTaskCache == null) {
            return null;
        }
        if (!runningTaskCache.isGatherTask()) {
            return null;
        }
        return findRuntimeByTaskId(runningTaskCache.getTaskId());
    }

    /**
     * 按任务ID读取采集运行态。
     *
     * @param taskId 任务ID
     * @return 采集运行态缓存，不存在时返回 null
     */
    public PlayerGatherTaskRuntimeCache findRuntimeByTaskId(Long taskId) {
        if (taskId == null) {
            return null;
        }
        String key = ActionTaskRedisKeys.buildGatherRuntimeKey(taskId);
        return readJson(key, PlayerGatherTaskRuntimeCache.class);
    }

    /**
     * 按任务ID读取当前 segment plan。
     *
     * @param taskId 任务ID
     * @return segment plan 缓存，不存在时返回 null
     */
    public GatherTaskSegmentPlanCache findSegmentPlanByTaskId(Long taskId) {
        if (taskId == null) {
            return null;
        }
        String key = ActionTaskRedisKeys.buildGatherSegmentPlanKey(taskId);
        return readJson(key, GatherTaskSegmentPlanCache.class);
    }

    /**
     * 按任务ID读取 pending_reward_pool。
     *
     * @param taskId 任务ID
     * @return pending_reward_pool 缓存，不存在时返回 null
     */
    public GatherTaskPendingRewardPoolCache findPendingRewardPoolByTaskId(Long taskId) {
        if (taskId == null) {
            return null;
        }
        String key = ActionTaskRedisKeys.buildGatherPendingRewardPoolKey(taskId);
        return readJson(key, GatherTaskPendingRewardPoolCache.class);
    }

    /**
     * 保存玩家当前运行槽位。
     *
     * @param cache 运行槽位缓存
     */
    public void saveRunningTask(PlayerRunningTaskCache cache) {
        validateRunningTaskCache(cache);
        String key = ActionTaskRedisKeys.buildPlayerRunningTaskKey(cache.getPlayerId());
        writeJson(key, cache, ActionTaskRedisKeys.PLAYER_RUNNING_TASK_TTL);
    }

    /**
     * 保存采集运行态。
     *
     * @param cache 采集运行态缓存
     */
    public void saveRuntime(PlayerGatherTaskRuntimeCache cache) {
        validateRuntimeCache(cache);
        String key = ActionTaskRedisKeys.buildGatherRuntimeKey(cache.getTaskId());
        writeJson(key, cache, ActionTaskRedisKeys.GATHER_RUNTIME_TTL);
    }

    /**
     * 保存当前 segment plan。
     *
     * @param cache segment plan 缓存
     */
    public void saveSegmentPlan(GatherTaskSegmentPlanCache cache) {
        validateSegmentPlanCache(cache);
        String key = ActionTaskRedisKeys.buildGatherSegmentPlanKey(cache.getTaskId());
        writeJson(key, cache, ActionTaskRedisKeys.GATHER_SEGMENT_PLAN_TTL);
    }

    /**
     * 保存 pending_reward_pool。
     *
     * @param cache pending_reward_pool 缓存
     */
    public void savePendingRewardPool(GatherTaskPendingRewardPoolCache cache) {
        validatePendingRewardPoolCache(cache);
        String key = ActionTaskRedisKeys.buildGatherPendingRewardPoolKey(cache.getTaskId());
        writeJson(key, cache, ActionTaskRedisKeys.GATHER_PENDING_REWARD_POOL_TTL);
    }

    /**
     * 删除玩家当前运行槽位。
     *
     * @param playerId 玩家ID
     */
    public void deleteRunningTask(Long playerId) {
        if (playerId == null) {
            return;
        }
        String key = ActionTaskRedisKeys.buildPlayerRunningTaskKey(playerId);
        stringRedisTemplate.delete(key);
    }

    /**
     * 仅当槽位中的 taskId 与传入 taskId 一致时，才删除玩家当前运行槽位。
     *
     * @param playerId 玩家ID
     * @param taskId 任务ID
     */
    public void deleteRunningTaskIfMatch(Long playerId, Long taskId) {
        if (playerId == null || taskId == null) {
            return;
        }

        PlayerRunningTaskCache cache = findRunningTaskByPlayerId(playerId);
        if (cache == null) {
            return;
        }
        if (!taskId.equals(cache.getTaskId())) {
            return;
        }

        deleteRunningTask(playerId);
    }

    /**
     * 删除采集运行态。
     *
     * @param taskId 任务ID
     */
    public void deleteRuntime(Long taskId) {
        if (taskId == null) {
            return;
        }
        String key = ActionTaskRedisKeys.buildGatherRuntimeKey(taskId);
        stringRedisTemplate.delete(key);
    }

    /**
     * 删除当前 segment plan。
     *
     * @param taskId 任务ID
     */
    public void deleteSegmentPlan(Long taskId) {
        if (taskId == null) {
            return;
        }
        String key = ActionTaskRedisKeys.buildGatherSegmentPlanKey(taskId);
        stringRedisTemplate.delete(key);
    }

    /**
     * 删除 pending_reward_pool。
     *
     * @param taskId 任务ID
     */
    public void deletePendingRewardPool(Long taskId) {
        if (taskId == null) {
            return;
        }
        String key = ActionTaskRedisKeys.buildGatherPendingRewardPoolKey(taskId);
        stringRedisTemplate.delete(key);
    }

    /**
     * 删除一个采集任务对应的全部热态 key。
     *
     * @param playerId 玩家ID
     * @param taskId 任务ID
     */
    public void deleteTaskHotState(Long playerId, Long taskId) {
        deleteRunningTaskIfMatch(playerId, taskId);
        deleteRuntime(taskId);
        deleteSegmentPlan(taskId);
        deletePendingRewardPool(taskId);
    }

    /**
     * 判断是否应该缓存当前 segment plan。
     *
     * @param task 采集任务
     * @return true-应该缓存，false-不需要缓存
     */
    private boolean shouldCacheSegmentPlan(PlayerGatherTask task) {
        if (task == null) {
            return false;
        }
        if (task.getCurrentSegmentStart() != null && task.getCurrentSegmentEnd() != null) {
            return true;
        }
        return StringUtils.hasText(task.getCurrentSegmentRewardPlanJson());
    }

    /**
     * 判断是否应该缓存 pending_reward_pool。
     *
     * @param task 采集任务
     * @return true-应该缓存，false-不需要缓存
     */
    private boolean shouldCachePendingRewardPool(PlayerGatherTask task) {
        if (task == null) {
            return false;
        }
        if (StringUtils.hasText(task.getPendingRewardPoolJson())) {
            return true;
        }

        Long completedCount = task.getCompletedCount();
        Long flushedCount = task.getFlushedCount();
        if (completedCount == null || flushedCount == null) {
            return false;
        }
        return completedCount.longValue() > flushedCount.longValue();
    }

    /**
     * 写入 JSON 到 Redis。
     *
     * @param key Redis key
     * @param value 值对象
     * @param ttl 过期时间
     */
    private void writeJson(String key, Object value, java.time.Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Redis 热态对象序列化失败，key=" + key, e);
        }
    }

    /**
     * 从 Redis 读取 JSON 并反序列化。
     *
     * @param key Redis key
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 反序列化结果，不存在时返回 null
     */
    private <T> T readJson(String key, Class<T> clazz) {
        String json = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new IllegalStateException("Redis 热态对象反序列化失败，key=" + key, e);
        }
    }

    /**
     * 校验采集任务基础参数。
     *
     * @param task 采集任务
     */
    private void validateTask(PlayerGatherTask task) {
        if (task == null) {
            throw new IllegalArgumentException("采集任务不能为空");
        }
        if (task.getId() == null) {
            throw new IllegalArgumentException("采集任务 taskId 不能为空");
        }
        if (task.getPlayerId() == null) {
            throw new IllegalArgumentException("采集任务 playerId 不能为空");
        }
    }

    /**
     * 校验运行槽位缓存。
     *
     * @param cache 运行槽位缓存
     */
    private void validateRunningTaskCache(PlayerRunningTaskCache cache) {
        if (cache == null) {
            throw new IllegalArgumentException("运行槽位缓存不能为空");
        }
        if (cache.getTaskId() == null) {
            throw new IllegalArgumentException("运行槽位缓存 taskId 不能为空");
        }
        if (cache.getPlayerId() == null) {
            throw new IllegalArgumentException("运行槽位缓存 playerId 不能为空");
        }
    }

    /**
     * 校验采集运行态缓存。
     *
     * @param cache 采集运行态缓存
     */
    private void validateRuntimeCache(PlayerGatherTaskRuntimeCache cache) {
        if (cache == null) {
            throw new IllegalArgumentException("采集运行态缓存不能为空");
        }
        if (cache.getTaskId() == null) {
            throw new IllegalArgumentException("采集运行态缓存 taskId 不能为空");
        }
        if (cache.getPlayerId() == null) {
            throw new IllegalArgumentException("采集运行态缓存 playerId 不能为空");
        }
    }

    /**
     * 校验 segment plan 缓存。
     *
     * @param cache segment plan 缓存
     */
    private void validateSegmentPlanCache(GatherTaskSegmentPlanCache cache) {
        if (cache == null) {
            throw new IllegalArgumentException("segment plan 缓存不能为空");
        }
        if (cache.getTaskId() == null) {
            throw new IllegalArgumentException("segment plan 缓存 taskId 不能为空");
        }
        if (cache.getPlayerId() == null) {
            throw new IllegalArgumentException("segment plan 缓存 playerId 不能为空");
        }
    }

    /**
     * 校验 pending_reward_pool 缓存。
     *
     * @param cache pending_reward_pool 缓存
     */
    private void validatePendingRewardPoolCache(GatherTaskPendingRewardPoolCache cache) {
        if (cache == null) {
            throw new IllegalArgumentException("pending_reward_pool 缓存不能为空");
        }
        if (cache.getTaskId() == null) {
            throw new IllegalArgumentException("pending_reward_pool 缓存 taskId 不能为空");
        }
        if (cache.getPlayerId() == null) {
            throw new IllegalArgumentException("pending_reward_pool 缓存 playerId 不能为空");
        }
    }
}