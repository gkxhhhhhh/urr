package com.urr.app.action.task.result;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 正式库存消耗前的准备结果。
 *
 * 说明：
 * 1. 这里只表达“消耗前统一 flush”这一步的汇总结果。
 * 2. 不直接表达具体卖出/制造/任务提交业务结果。
 * 3. 上层正式库存消耗服务可拿它做日志、排障和链路观测。
 */
@Data
public class PrepareConsumeInventoryResult {

    /**
     * 玩家ID。
     */
    private Long playerId;

    /**
     * 本次准备时刻。
     */
    private LocalDateTime operateTime;

    /**
     * flush 前待处理的 pending 任务数量。
     *
     * 说明：
     * 1. 这个值是在“必要 advance 完成后、正式 flush 开始前”统计的。
     * 2. 它更接近真正参与本次准备流程的任务数。
     */
    private Integer beforePendingTaskCount;

    /**
     * flush 后剩余的 pending 任务数量。
     */
    private Integer afterPendingTaskCount;

    /**
     * 本次 advance 过的运行中采集任务数量。
     */
    private Integer advancedTaskCount;

    /**
     * 本次 advance 新增完成的轮次数。
     */
    private Long advancedRoundCount;

    /**
     * 本次实际发生 flush 的任务数量。
     */
    private Integer flushedTaskCount;

    /**
     * 本次实际推进的 flushed 总轮次数。
     */
    private Long flushedRoundCount;

    /**
     * 本次实际正式入库的奖励条目总数。
     */
    private Integer appliedRewardEntryCount;

    /**
     * 本次是否真的发生了正式库存 flush。
     */
    private Boolean rewardFlushed;

    /**
     * 本次是否已经完成“库存消耗前准备”。
     */
    private Boolean inventoryPrepared;
}