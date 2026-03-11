package com.urr.app.action.task;

import com.urr.domain.action.task.GatherTaskRewardEntry;
import com.urr.domain.action.task.GatherTaskStatSnapshot;
import com.urr.domain.action.task.PlayerGatherTask;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 采集任务奖励生成器。
 *
 * 说明：
 * 1. 当前仓库里，采集掉落的完整真相配置还没有正式接到 Java 侧。
 * 2. 本会话先做一个最小、确定性的锁定奖励生成器。
 * 3. 生成规则只依赖 rewardSeed + roundIndex + 快照，不依赖 Redis 临时状态。
 */
@Component
public class GatherTaskRewardGenerator {

    /**
     * 奖励类型：物品。
     */
    public static final String REWARD_TYPE_ITEM = "ITEM";

    /**
     * 当前最小闭环下的采集动作与产出物映射。
     */
    private static final Map<String, String> ACTION_ITEM_CODE_MAP = new HashMap<String, String>();

    static {
        ACTION_ITEM_CODE_MAP.put("GATHER_PICKING_EGG", "FOOD_EGG");
        ACTION_ITEM_CODE_MAP.put("GATHER_PICKING_COTTON", "MAT_COTTON");
        ACTION_ITEM_CODE_MAP.put("GATHER_PICKING_COFFEE_BEAN", "FOOD_COFFEE_BEAN");
    }

    /**
     * 生成某一轮的锁定奖励。
     *
     * @param task 采集任务
     * @param roundIndex 轮次序号，建议使用 1-based
     * @return 该轮奖励列表
     */
    public List<GatherTaskRewardEntry> generateRoundRewards(PlayerGatherTask task, long roundIndex) {
        validateTask(task);

        List<GatherTaskRewardEntry> rewardList = new ArrayList<GatherTaskRewardEntry>();
        String itemCode = resolveItemCode(task.getActionCode());
        long quantity = calculateItemQuantity(task.getStatSnapshot(), task.getRewardSeed(), roundIndex);
        if (quantity <= 0L) {
            return rewardList;
        }

        GatherTaskRewardEntry rewardEntry = new GatherTaskRewardEntry();
        rewardEntry.setRewardType(REWARD_TYPE_ITEM);
        rewardEntry.setRewardCode(itemCode);
        rewardEntry.setQuantity(quantity);
        rewardList.add(rewardEntry);
        return rewardList;
    }

    /**
     * 解析当前采集动作对应的产出物编码。
     *
     * @param actionCode 动作编码
     * @return 物品编码
     */
    public String resolveItemCode(String actionCode) {
        if (!StringUtils.hasText(actionCode)) {
            throw new IllegalArgumentException("actionCode不能为空");
        }

        String itemCode = ACTION_ITEM_CODE_MAP.get(actionCode.trim());
        if (!StringUtils.hasText(itemCode)) {
            throw new IllegalStateException("当前采集动作还没有最小奖励映射，actionCode=" + actionCode);
        }
        return itemCode;
    }

    /**
     * 计算一轮采集的物品数量。
     *
     * 说明：
     * 1. 当前最小规则是“每轮至少 1 个基础产物”。
     * 2. gatherEfficiency 大于 1 的小数部分，会通过固定种子做确定性加一。
     * 3. 这样既能表达效率加成，也能保证相同 seed + roundIndex 下结果稳定。
     *
     * @param snapshot 采集快照
     * @param rewardSeed 奖励种子
     * @param roundIndex 轮次序号
     * @return 本轮产出数量
     */
    private long calculateItemQuantity(GatherTaskStatSnapshot snapshot, Long rewardSeed, long roundIndex) {
        BigDecimal efficiency = BigDecimal.ONE;
        if (snapshot != null && snapshot.getGatherEfficiency() != null
                && snapshot.getGatherEfficiency().compareTo(BigDecimal.ONE) > 0) {
            efficiency = snapshot.getGatherEfficiency();
        }

        long quantity = efficiency.longValue();
        if (quantity <= 0L) {
            quantity = 1L;
        }

        BigDecimal fraction = efficiency.subtract(BigDecimal.valueOf(quantity));
        if (fraction.compareTo(BigDecimal.ZERO) > 0) {
            long threshold = fraction.multiply(BigDecimal.valueOf(1000000L)).longValue();
            long roll = nextPositiveValue(rewardSeed, roundIndex) % 1000000L;
            if (roll < threshold) {
                quantity++;
            }
        }
        return quantity;
    }

    /**
     * 生成固定的正整数随机值。
     *
     * @param rewardSeed 奖励种子
     * @param roundIndex 轮次序号
     * @return 正整数值
     */
    private long nextPositiveValue(Long rewardSeed, long roundIndex) {
        long seed = rewardSeed == null ? 1L : rewardSeed.longValue();
        long value = seed + roundIndex * 1103515245L + 12345L;
        value ^= (value << 13);
        value ^= (value >>> 7);
        value ^= (value << 17);
        return value & Long.MAX_VALUE;
    }

    /**
     * 校验采集任务是否具备奖励生成所需的最小字段。
     *
     * @param task 采集任务
     */
    private void validateTask(PlayerGatherTask task) {
        if (task == null) {
            throw new IllegalArgumentException("采集任务不能为空");
        }
        if (!StringUtils.hasText(task.getActionCode())) {
            throw new IllegalArgumentException("采集任务 actionCode 不能为空");
        }
        if (task.getStatSnapshot() == null) {
            throw new IllegalArgumentException("采集任务 statSnapshot 不能为空");
        }
    }
}