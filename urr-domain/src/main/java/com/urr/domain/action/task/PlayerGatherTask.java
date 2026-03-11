package com.urr.domain.action.task;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色采集任务模型。
 *
 * 说明：
 * 1. 这是“动作任务”的采集特化版本。
 * 2. 通用运行态字段放在 PlayerActionTask。
 * 3. 采集专属字段，例如轮次、分段、快照、奖励计划、待刷池，全部放在这里。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlayerGatherTask extends PlayerActionTask {

    /**
     * 目标轮次。
     * -1 表示无限次数。
     */
    private Long targetCount;

    /**
     * 已完成轮次。
     *
     * 语义：
     * 1. completedCount 表示逻辑上已经完成的轮次。
     * 2. 这些轮次的奖励在业务语义上已经归属玩家。
     * 3. 但它们不一定已经正式刷入正式库存。
     * 4. 本次推进时只推进新增完成部分，不重复结算已完成部分。
     */
    private Long completedCount;

    /**
     * 已正式刷入正式库存的轮次。
     *
     * 语义：
     * 1. flushedCount 表示已经完成正式入库的轮次。
     * 2. 当 completedCount > flushedCount 时，说明仍有收益停留在“已完成未正式入库”的状态。
     */
    private Long flushedCount;

    /**
     * 当前分段开始轮次（含）。
     * 建议使用 1-based 轮次语义。
     */
    private Long currentSegmentStart;

    /**
     * 当前分段结束轮次（含）。
     * 建议使用 1-based 轮次语义。
     */
    private Long currentSegmentEnd;

    /**
     * 分段大小。
     * 当前采集默认 10。
     */
    private Integer segmentSize;

    /**
     * 采集任务启动时锁定的属性快照。
     */
    private GatherTaskStatSnapshot statSnapshot;

    /**
     * 当前分段锁定奖励计划。
     *
     * 说明：
     * 1. 这里表达的是“锁定计划”，不是估算。
     * 2. 本会话已经支持“当前 segment 的最小锁定计划”。
     * 3. 它只服务当前段，不做多段批量预生成。
     */
    private String currentSegmentRewardPlanJson;

    /**
     * 已完成但尚未正式刷入正式库存的收益池。
     *
     * 说明：
     * 1. 这里表达的是“已完成但未正式入库”的聚合收益。
     * 2. 本会话只聚合到 pending_reward_pool，不会正式写入背包/钱包。
     * 3. 后续任何会消耗库存的行为，都应先 flush 这部分收益。
     */
    private String pendingRewardPoolJson;

    /**
     * 创建采集任务默认模型。
     */
    public PlayerGatherTask() {
        this.setTaskType(ActionTaskTypeEnum.GATHER);
        this.setSegmentSize(ActionTaskConstants.DEFAULT_SEGMENT_SIZE);
    }

    /**
     * 判断当前任务是否为无限次数任务。
     *
     * @return true-无限次数，false-有限次数
     */
    public boolean isInfiniteTarget() {
        return targetCount != null && targetCount == ActionTaskConstants.INFINITE_TARGET_COUNT;
    }

    /**
     * 判断当前有限次数任务是否已经达到目标轮次。
     *
     * @return true-已达到，false-未达到
     */
    public boolean isFinishedByTargetCount() {
        if (isInfiniteTarget()) {
            return false;
        }
        if (targetCount == null || targetCount <= 0) {
            return false;
        }
        return getSafeCompletedCount() >= targetCount;
    }

    /**
     * 判断当前是否存在“已完成但未正式刷入正式库存”的轮次。
     *
     * @return true-存在，false-不存在
     */
    public boolean hasPendingRewardToFlush() {
        return getPendingRewardRoundCount() > 0;
    }

    /**
     * 获取“已完成但未正式刷入正式库存”的轮次数。
     *
     * @return 待刷入轮次数
     */
    public long getPendingRewardRoundCount() {
        long completed = getSafeCompletedCount();
        long flushed = getSafeFlushedCount();
        long pending = completed - flushed;
        return Math.max(pending, 0L);
    }

    /**
     * 判断当前是否已经带有采集快照。
     *
     * @return true-有快照，false-无快照
     */
    public boolean hasStatSnapshot() {
        return statSnapshot != null;
    }

    /**
     * 判断当前是否已经锁定了当前分段奖励计划。
     *
     * @return true-已锁定，false-未锁定
     */
    public boolean hasLockedRewardPlan() {
        return currentSegmentRewardPlanJson != null && !currentSegmentRewardPlanJson.trim().isEmpty();
    }

    /**
     * 判断当前是否已经存在待刷入收益池。
     *
     * @return true-存在，false-不存在
     */
    public boolean hasPendingRewardPool() {
        return pendingRewardPoolJson != null && !pendingRewardPoolJson.trim().isEmpty();
    }

    /**
     * 获取安全的 completedCount。
     *
     * @return completedCount，空时返回 0
     */
    public long getSafeCompletedCount() {
        return completedCount == null ? 0L : completedCount;
    }

    /**
     * 获取安全的 flushedCount。
     *
     * @return flushedCount，空时返回 0
     */
    public long getSafeFlushedCount() {
        return flushedCount == null ? 0L : flushedCount;
    }
}