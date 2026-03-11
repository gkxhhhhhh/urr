package com.urr.domain.action.task;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采集任务当前 segment plan 热缓存。
 *
 * 说明：
 * 1. 这里只承载“当前 10 次一段”的锁定计划。
 * 2. 计划明细本身继续使用 planJson 整体承载，避免当前会话过早固化奖励明细结构。
 * 3. 这个对象是热态镜像，数据库仍然保留 currentSegmentRewardPlanJson 作为恢复依据。
 */
@Data
public class GatherTaskSegmentPlanCache {

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
     * 当前 segment 开始轮次（含）。
     */
    private Long segmentStart;

    /**
     * 当前 segment 结束轮次（含）。
     */
    private Long segmentEnd;

    /**
     * segment 大小。
     */
    private Integer segmentSize;

    /**
     * 当前 segment 使用的奖励种子基准。
     */
    private Long rewardSeed;

    /**
     * 当前 segment 的锁定计划 JSON。
     *
     * 说明：
     * 1. 这里表达的是“锁定计划”，不是估算。
     * 2. 本会话已经支持最小的当前段锁定奖励计划。
     */
    private String planJson;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 最近更新时间。
     */
    private LocalDateTime updatedAt;

    /**
     * 判断当前是否存在计划 JSON。
     *
     * @return true-有，false-无
     */
    public boolean hasPlanJson() {
        return planJson != null && !planJson.trim().isEmpty();
    }
}