package com.urr.app.action.task;

import com.urr.domain.action.task.GatherTaskRewardEntry;
import com.urr.domain.action.task.GatherTaskRewardPool;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * pending_reward_pool 聚合器。
 *
 * 说明：
 * 1. 这里只做最小聚合能力。
 * 2. 聚合规则固定为：同 rewardType + rewardCode 的数量累加。
 */
@Component
public class GatherTaskPendingRewardAggregator {

    /**
     * 把新增奖励聚合进收益池。
     *
     * @param rewardPool 收益池
     * @param newRewards 新增奖励
     */
    public void mergeRewards(GatherTaskRewardPool rewardPool, List<GatherTaskRewardEntry> newRewards) {
        if (rewardPool == null || newRewards == null || newRewards.isEmpty()) {
            return;
        }

        for (int i = 0; i < newRewards.size(); i++) {
            mergeSingleReward(rewardPool, newRewards.get(i));
        }
    }

    /**
     * 把单条奖励聚合进收益池。
     *
     * @param rewardPool 收益池
     * @param rewardEntry 奖励项
     */
    private void mergeSingleReward(GatherTaskRewardPool rewardPool, GatherTaskRewardEntry rewardEntry) {
        if (rewardEntry == null) {
            return;
        }
        if (!StringUtils.hasText(rewardEntry.getRewardType())) {
            return;
        }
        if (!StringUtils.hasText(rewardEntry.getRewardCode())) {
            return;
        }
        if (rewardEntry.getQuantity() == null || rewardEntry.getQuantity() <= 0L) {
            return;
        }

        List<GatherTaskRewardEntry> rewardList = rewardPool.getSafeRewardList();
        for (int i = 0; i < rewardList.size(); i++) {
            GatherTaskRewardEntry current = rewardList.get(i);
            if (isSameReward(current, rewardEntry)) {
                long mergedQuantity = safeQuantity(current.getQuantity()) + safeQuantity(rewardEntry.getQuantity());
                current.setQuantity(mergedQuantity);
                return;
            }
        }

        GatherTaskRewardEntry copied = new GatherTaskRewardEntry();
        copied.setRewardType(rewardEntry.getRewardType());
        copied.setRewardCode(rewardEntry.getRewardCode());
        copied.setQuantity(rewardEntry.getQuantity());
        rewardList.add(copied);
    }

    /**
     * 判断两条奖励是否属于同一种奖励。
     *
     * @param left 左侧奖励
     * @param right 右侧奖励
     * @return true-相同，false-不同
     */
    private boolean isSameReward(GatherTaskRewardEntry left, GatherTaskRewardEntry right) {
        if (left == null || right == null) {
            return false;
        }
        if (!safeEquals(left.getRewardType(), right.getRewardType())) {
            return false;
        }
        return safeEquals(left.getRewardCode(), right.getRewardCode());
    }

    /**
     * 安全比较字符串。
     *
     * @param left 左值
     * @param right 右值
     * @return true-相等，false-不等
     */
    private boolean safeEquals(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    /**
     * 获取安全数量。
     *
     * @param quantity 数量
     * @return 安全数量
     */
    private long safeQuantity(Long quantity) {
        return quantity == null ? 0L : quantity.longValue();
    }
}