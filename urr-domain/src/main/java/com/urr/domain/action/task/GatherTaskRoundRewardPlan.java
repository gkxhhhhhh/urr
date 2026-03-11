package com.urr.domain.action.task;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单轮锁定奖励计划。
 */
@Data
public class GatherTaskRoundRewardPlan {

    /**
     * 轮次序号。
     * 建议使用 1-based。
     */
    private Long roundIndex;

    /**
     * 该轮锁定奖励。
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