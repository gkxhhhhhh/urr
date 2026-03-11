package com.urr.app.action.task;

import com.urr.domain.action.task.ActionTaskConstants;
import com.urr.domain.action.task.GatherTaskRewardEntry;
import com.urr.domain.action.task.GatherTaskRoundRewardPlan;
import com.urr.domain.action.task.GatherTaskSegmentRewardPlan;
import com.urr.domain.action.task.PlayerGatherTask;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 采集任务 segment 规划器。
 *
 * 说明：
 * 1. 当前只做“当前段 / 下一段”的最小续接。
 * 2. 不做复杂的多段批量预生成。
 */
@Component
public class GatherTaskSegmentPlanner {

    /**
     * 奖励生成器。
     */
    private final GatherTaskRewardGenerator gatherTaskRewardGenerator;

    /**
     * 构造方法。
     *
     * @param gatherTaskRewardGenerator 奖励生成器
     */
    public GatherTaskSegmentPlanner(GatherTaskRewardGenerator gatherTaskRewardGenerator) {
        this.gatherTaskRewardGenerator = gatherTaskRewardGenerator;
    }

    /**
     * 根据当前任务状态，构建“当前待跑 segment”的锁定计划。
     *
     * @param task 采集任务
     * @return segment 计划；如果任务后续已无待跑轮次，则返回 null
     */
    public GatherTaskSegmentRewardPlan buildCurrentSegmentPlan(PlayerGatherTask task) {
        if (task == null) {
            return null;
        }

        long nextRoundIndex = task.getSafeCompletedCount() + 1L;
        if (!task.isInfiniteTarget() && task.getTargetCount() != null && nextRoundIndex > task.getTargetCount()) {
            return null;
        }

        int segmentSize = resolveSegmentSize(task.getSegmentSize());
        long segmentStart = ((nextRoundIndex - 1L) / segmentSize) * segmentSize + 1L;
        long segmentEnd = segmentStart + segmentSize - 1L;
        if (!task.isInfiniteTarget() && task.getTargetCount() != null && task.getTargetCount() > 0L) {
            segmentEnd = Math.min(segmentEnd, task.getTargetCount());
        }

        GatherTaskSegmentRewardPlan plan = new GatherTaskSegmentRewardPlan();
        plan.setSegmentStart(segmentStart);
        plan.setSegmentEnd(segmentEnd);
        plan.setSegmentSize(segmentSize);
        plan.setRewardSeed(task.getRewardSeed());

        for (long roundIndex = segmentStart; roundIndex <= segmentEnd; roundIndex++) {
            GatherTaskRoundRewardPlan roundPlan = new GatherTaskRoundRewardPlan();
            roundPlan.setRoundIndex(roundIndex);

            List<GatherTaskRewardEntry> rewardList = gatherTaskRewardGenerator.generateRoundRewards(task, roundIndex);
            roundPlan.setRewardList(rewardList);
            plan.getSafeRoundPlanList().add(roundPlan);
        }
        return plan;
    }

    /**
     * 解析安全的分段大小。
     *
     * @param segmentSize 分段大小
     * @return 安全分段大小
     */
    private int resolveSegmentSize(Integer segmentSize) {
        if (segmentSize == null || segmentSize.intValue() <= 0) {
            return ActionTaskConstants.DEFAULT_SEGMENT_SIZE;
        }
        return segmentSize.intValue();
    }
}