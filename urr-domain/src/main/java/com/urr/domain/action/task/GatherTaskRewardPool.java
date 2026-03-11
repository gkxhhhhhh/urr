package com.urr.domain.action.task;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 采集任务待刷入收益池。
 *
 * 说明：
 * 1. 这里表达的是“已完成但未正式入库”的聚合收益。
 * 2. 后续 flush 时，正式库存只需要消费这里的聚合结果即可。
 */
@Data
public class GatherTaskRewardPool {

    /**
     * 聚合后的奖励列表。
     */
    private List<GatherTaskRewardEntry> rewardList;

    /**
     * 获取安全的奖励列表。
     *
     * @return 奖励列表
     */
    public List<GatherTaskRewardEntry> getSafeRewardList() {
        if (rewardList == null) {
            rewardList = new ArrayList<GatherTaskRewardEntry>();
        }
        return rewardList;
    }
}