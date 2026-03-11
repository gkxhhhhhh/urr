package com.urr.domain.action.task;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采集任务 pending_reward_pool 热缓存。
 *
 * 说明：
 * 1. 这里表达的是“已完成但未正式入库”的收益池。
 * 2. 收益池内部结构继续使用 rewardPoolJson 整体承载，避免当前会话过早锁死收益明细模型。
 * 3. Redis 只是热态镜像，数据库中的 pendingRewardPoolJson 仍然是恢复依据。
 */
@Data
public class GatherTaskPendingRewardPoolCache {

    /**
     * 任务ID。
     */
    private Long taskId;

    /**
     * 玩家ID。
     */
    private Long playerId;

    /**
     * 区服ID。
     */
    private Integer serverId;

    /**
     * 当前已完成轮次。
     */
    private Long completedCount;

    /**
     * 当前已正式刷库轮次。
     */
    private Long flushedCount;

    /**
     * 当前待刷库轮次数。
     */
    private Long pendingRoundCount;

    /**
     * 待刷收益池 JSON。
     */
    private String rewardPoolJson;

    /**
     * 最近更新时间。
     */
    private LocalDateTime updatedAt;

    /**
     * 判断当前是否带有待刷收益池内容。
     *
     * @return true-有，false-无
     */
    public boolean hasRewardPoolJson() {
        return rewardPoolJson != null && !rewardPoolJson.trim().isEmpty();
    }
}