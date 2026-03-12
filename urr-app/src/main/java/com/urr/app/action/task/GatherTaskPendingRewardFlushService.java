package com.urr.app.action.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urr.app.action.task.result.PendingRewardFlushResult;
import com.urr.domain.action.task.GatherTaskPendingRewardPoolCache;
import com.urr.domain.action.task.GatherTaskRewardPool;
import com.urr.domain.action.task.PlayerGatherTask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * 采集任务 pending_reward_pool flush 服务。
 *
 * 说明：
 * 1. flush 的对象是“已完成但未正式入库”的收益池。
 * 2. flush 成功后，正式库存写入正式表。
 * 3. flush 成功后，flushedCount 会推进到 completedCount。
 * 4. flush 成功后，pending_reward_pool 会清空。
 * 5. flush 本身不负责 stop；它只是一个可复用的最小正式入库能力。
 */
@Service
@RequiredArgsConstructor
public class GatherTaskPendingRewardFlushService {

    /**
     * 采集任务 DB 仓储。
     */
    private final PlayerGatherTaskRepository playerGatherTaskRepository;

    /**
     * 采集任务 Redis 热态仓储。
     */
    private final PlayerGatherTaskRedisRepository playerGatherTaskRedisRepository;

    /**
     * 正式库存入库适配器。
     */
    private final GatherTaskRewardMaterializer gatherTaskRewardMaterializer;

    /**
     * JSON 编解码器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 执行一次 pending_reward_pool flush。
     *
     * @param taskId 任务ID
     * @return flush 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public PendingRewardFlushResult flushPendingReward(Long taskId) {
        validateTaskId(taskId);

        PlayerGatherTask task = requireTask(taskId);
        GatherTaskPendingRewardPoolCache pendingCache = playerGatherTaskRedisRepository.findPendingRewardPoolByTaskId(taskId);

        PendingRewardFlushResult result = buildBaseResult(task);
        long flushedRoundCount = task.getPendingRewardRoundCount();

        if (flushedRoundCount <= 0L) {
            clearStaleRewardPoolIfNecessary(task, pendingCache);
            result.setAfterCompletedCount(task.getSafeCompletedCount());
            result.setAfterFlushedCount(task.getSafeFlushedCount());
            result.setFlushedRoundCount(0L);
            result.setAppliedRewardEntryCount(0);
            result.setRewardFlushed(Boolean.FALSE);
            return result;
        }

        GatherTaskRewardPool rewardPool = readPendingRewardPool(task, pendingCache);
        int appliedRewardEntryCount = gatherTaskRewardMaterializer.materialize(task, rewardPool);

        task.setFlushedCount(task.getSafeCompletedCount());
        task.setPendingRewardPoolJson(null);
        playerGatherTaskRepository.update(task);
        refreshRedisAfterFlush(task);

        result.setAfterCompletedCount(task.getSafeCompletedCount());
        result.setAfterFlushedCount(task.getSafeFlushedCount());
        result.setFlushedRoundCount(flushedRoundCount);
        result.setAppliedRewardEntryCount(appliedRewardEntryCount);
        result.setRewardFlushed(Boolean.TRUE);
        return result;
    }

    /**
     * 从 DB 真相层和 Redis 热态层读取收益池。
     *
     * 规则：
     * 1. 以任务真相层中的 pendingRewardPoolJson 为主。
     * 2. DB 为空时，允许用 Redis 热态做一次薄 fallback。
     * 3. 如果存在待刷轮次，但两边都没有收益池内容，则直接报错，避免把 flushedCount 错误推进。
     *
     * @param task 采集任务
     * @param pendingCache Redis 中的 pending 缓存
     * @return 待刷收益池
     */
    private GatherTaskRewardPool readPendingRewardPool(PlayerGatherTask task, GatherTaskPendingRewardPoolCache pendingCache) {
        String rewardPoolJson = task.getPendingRewardPoolJson();
        if (!StringUtils.hasText(rewardPoolJson) && pendingCache != null && pendingCache.hasRewardPoolJson()) {
            rewardPoolJson = pendingCache.getRewardPoolJson();
        }

        if (!StringUtils.hasText(rewardPoolJson)) {
            throw new IllegalStateException("存在待刷轮次，但 pending_reward_pool 为空，taskId=" + task.getId());
        }

        try {
            return objectMapper.readValue(rewardPoolJson, GatherTaskRewardPool.class);
        } catch (IOException e) {
            throw new IllegalStateException("pending_reward_pool 反序列化失败，taskId=" + task.getId(), e);
        }
    }

    /**
     * 清理“轮次已无待刷，但收益池仍残留”的脏数据。
     *
     * 说明：
     * 1. 如果 completedCount == flushedCount，就不应该再保留 pending_reward_pool。
     * 2. 这里不会再做正式库存写入，只做真相层和热态层清理。
     *
     * @param task 采集任务
     * @param pendingCache Redis 中的 pending 缓存
     */
    private void clearStaleRewardPoolIfNecessary(PlayerGatherTask task, GatherTaskPendingRewardPoolCache pendingCache) {
        boolean dbHasStalePool = task.hasPendingRewardPool();
        boolean redisHasStalePool = pendingCache != null && pendingCache.hasRewardPoolJson();

        if (!dbHasStalePool && !redisHasStalePool) {
            return;
        }

        if (dbHasStalePool) {
            task.setPendingRewardPoolJson(null);
            playerGatherTaskRepository.update(task);
        }

        if (task.isRunning()) {
            playerGatherTaskRedisRepository.saveTaskHotState(task);
        } else {
            playerGatherTaskRedisRepository.deletePendingRewardPool(task.getId());
        }
    }

    /**
     * flush 后刷新 Redis。
     *
     * @param task 采集任务
     */
    private void refreshRedisAfterFlush(PlayerGatherTask task) {
        if (task.isRunning()) {
            playerGatherTaskRedisRepository.saveTaskHotState(task);
            return;
        }
        playerGatherTaskRedisRepository.deletePendingRewardPool(task.getId());
    }

    /**
     * 查询采集任务。
     *
     * @param taskId 任务ID
     * @return 采集任务
     */
    private PlayerGatherTask requireTask(Long taskId) {
        PlayerGatherTask task = playerGatherTaskRepository.findByTaskId(taskId);
        if (task == null) {
            throw new IllegalArgumentException("采集任务不存在，taskId=" + taskId);
        }
        return task;
    }

    /**
     * 组装基础结果。
     *
     * @param task 采集任务
     * @return flush 结果
     */
    private PendingRewardFlushResult buildBaseResult(PlayerGatherTask task) {
        PendingRewardFlushResult result = new PendingRewardFlushResult();
        result.setTaskId(task.getId());
        result.setPlayerId(task.getPlayerId());
        result.setBeforeCompletedCount(task.getSafeCompletedCount());
        result.setBeforeFlushedCount(task.getSafeFlushedCount());
        result.setAfterCompletedCount(task.getSafeCompletedCount());
        result.setAfterFlushedCount(task.getSafeFlushedCount());
        result.setFlushedRoundCount(0L);
        result.setAppliedRewardEntryCount(0);
        result.setRewardFlushed(Boolean.FALSE);
        return result;
    }

    /**
     * 校验任务ID。
     *
     * @param taskId 任务ID
     */
    private void validateTaskId(Long taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("taskId不能为空");
        }
    }
}