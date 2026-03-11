package com.urr.domain.action.task;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 当前分段锁定奖励计划。
 *
 * 说明：
 * 1. 这里表达的是“当前 segment 的锁定计划”，不是估算。
 * 2. 当前会话只做最小结构，便于后续 stop/flush/query 直接复用。
 */
@Data
public class GatherTaskSegmentRewardPlan {

    /**
     * 分段开始轮次（含）。
     */
    private Long segmentStart;

    /**
     * 分段结束轮次（含）。
     */
    private Long segmentEnd;

    /**
     * 分段大小。
     */
    private Integer segmentSize;

    /**
     * 奖励随机种子。
     */
    private Long rewardSeed;

    /**
     * 分段内各轮的锁定奖励计划。
     */
    private List<GatherTaskRoundRewardPlan> roundPlanList;

    /**
     * 获取安全的轮次计划列表。
     *
     * @return 轮次计划列表
     */
    public List<GatherTaskRoundRewardPlan> getSafeRoundPlanList() {
        if (roundPlanList == null) {
            roundPlanList = new ArrayList<GatherTaskRoundRewardPlan>();
        }
        return roundPlanList;
    }
}