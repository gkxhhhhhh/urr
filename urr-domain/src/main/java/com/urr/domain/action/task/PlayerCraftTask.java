package com.urr.domain.action.task;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 角色制造任务模型。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlayerCraftTask extends PlayerActionTask {

    /**
     * 目标次数。
     * 0 或 -1 表示无限次数。
     */
    private Long targetCount;

    /**
     * 已完成次数。
     */
    private Long completedCount;

    /**
     * 配方快照 JSON。
     */
    private String recipeSnapshotJson;

    /**
     * 下一轮完成时间戳（毫秒）。
     */
    private Long nextRoundFinishTime;

    /**
     * 创建默认制造任务。
     */
    public PlayerCraftTask() {
        this.setTaskType(ActionTaskTypeEnum.CRAFT);
    }

    /**
     * 判断是否无限次数。
     *
     * @return true-无限，false-有限
     */
    public boolean isInfiniteTarget() {
        return targetCount == null
                || targetCount.longValue() == 0L
                || targetCount.longValue() == ActionTaskConstants.INFINITE_TARGET_COUNT;
    }

    /**
     * 判断是否已达到目标次数。
     *
     * @return true-已完成，false-未完成
     */
    public boolean isFinishedByTargetCount() {
        if (isInfiniteTarget()) {
            return false;
        }
        return getSafeCompletedCount() >= targetCount;
    }

    /**
     * 获取安全 completedCount。
     *
     * @return 已完成次数
     */
    public long getSafeCompletedCount() {
        return completedCount == null ? 0L : completedCount.longValue();
    }

    /**
     * 判断在指定时刻是否已经到达下一轮完成时间。
     *
     * @param settleTime 结算时刻
     * @return true-可结算一轮，false-不可结算
     */
    public boolean canSettleOneRound(LocalDateTime settleTime) {
        if (settleTime == null || nextRoundFinishTime == null) {
            return false;
        }
        if (!isRunning()) {
            return false;
        }
        if (isFinishedByTargetCount()) {
            return false;
        }
        long settleMillis = CraftTaskTimeSupport.toEpochMilli(settleTime);
        return settleMillis >= nextRoundFinishTime.longValue();
    }
}