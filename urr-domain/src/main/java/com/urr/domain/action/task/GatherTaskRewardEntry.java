package com.urr.domain.action.task;

import lombok.Data;

/**
 * 采集任务奖励项。
 *
 * 说明：
 * 1. 这是 action.task 领域内的最小奖励表达。
 * 2. 当前主要承接 pending_reward_pool 和 current_segment_reward_plan。
 * 3. 本会话先支持 ITEM，结构上同时兼容后续 CURRENCY。
 */
@Data
public class GatherTaskRewardEntry {

    /**
     * 奖励类型。
     * 例如：ITEM / CURRENCY。
     */
    private String rewardType;

    /**
     * 奖励编码。
     * ITEM 时一般是 itemCode，CURRENCY 时一般是 currencyCode。
     */
    private String rewardCode;

    /**
     * 数量。
     */
    private Long quantity;
}