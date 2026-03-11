package com.urr.domain.action.task;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 采集任务启动时锁定的属性快照。
 *
 * 说明：
 * 1. 本快照一旦生成，在本次采集任务生命周期内不应随装备变化而变化。
 * 2. 当前装备影响先用 json 字符串承载，避免这轮过度设计。
 * 3. rewardSeed 同时保留在快照和任务根上，便于快照自解释，也便于任务快速读取。
 */
@Data
public class GatherTaskStatSnapshot {

    /**
     * 当前采集等级。
     */
    private Integer gatherLevel;

    /**
     * 当前装备影响快照。
     * 建议存放已经算好的装备影响明细，而不是原始装备列表。
     */
    private String equipmentEffectSnapshotJson;

    /**
     * 当前采集耗时（毫秒/每轮）。
     */
    private Integer gatherDurationMs;

    /**
     * 当前采集效率。
     * 例如：1.00 表示基准效率，1.25 表示 125%。
     */
    private BigDecimal gatherEfficiency;

    /**
     * 当前允许离线时长（分钟）。
     */
    private Integer offlineMinutesLimit;

    /**
     * 奖励随机种子。
     */
    private Long rewardSeed;

    /**
     * 判断当前是否带有装备影响快照。
     *
     * @return true-有，false-无
     */
    public boolean hasEquipmentEffectSnapshot() {
        return equipmentEffectSnapshotJson != null && !equipmentEffectSnapshotJson.trim().isEmpty();
    }
}