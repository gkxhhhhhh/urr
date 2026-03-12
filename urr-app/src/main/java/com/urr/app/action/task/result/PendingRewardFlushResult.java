package com.urr.app.action.task.result;

import lombok.Data;

/**
 * pending_reward_pool flush 结果。
 *
 * 说明：
 * 1. flush 的对象是“已完成但未正式入库”的收益池。
 * 2. flush 成功后，正式库存会写入正式表。
 * 3. flush 成功后，flushedCount 会推进到 completedCount。
 * 4. flush 成功后，pending_reward_pool 会被清空。
 */
@Data
public class PendingRewardFlushResult {

    /**
     * 任务ID。
     */
    private Long taskId;

    /**
     * 玩家ID。
     */
    private Long playerId;

    /**
     * flush 前 completedCount。
     */
    private Long beforeCompletedCount;

    /**
     * flush 前 flushedCount。
     */
    private Long beforeFlushedCount;

    /**
     * flush 后 completedCount。
     */
    private Long afterCompletedCount;

    /**
     * flush 后 flushedCount。
     */
    private Long afterFlushedCount;

    /**
     * 本次实际推进的 flushed 轮次数。
     */
    private Long flushedRoundCount;

    /**
     * 本次实际落正式库存的奖励条目数。
     */
    private Integer appliedRewardEntryCount;

    /**
     * 本次是否真的发生了正式库存写入。
     */
    private Boolean rewardFlushed;
}