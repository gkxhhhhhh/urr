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
 * 4. 本次把产物、经验、爆率、暴击率一起锁进快照，避免任务运行中被后续配置变更影响。
 * 5. 为兼容混采，新增 rewardListJson；单采继续兼容 itemCode。
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
     * 本任务锁定的产出物编码。
     * 单采沿用该字段；混采时允许为空。
     */
    private String itemCode;

    /**
     * 本任务锁定的奖励列表 JSON。
     * 混采使用该字段；单采也可同步写入，便于统一处理。
     */
    private String rewardListJson;

    /**
     * 本任务锁定的技能编码。
     */
    private String skillCode;

    /**
     * 本任务每轮锁定经验。
     */
    private Integer expGain;

    /**
     * 本任务锁定暴击率。
     * 单位：百分比，例如 150 代表 150%。
     */
    private BigDecimal criticalRate;

    /**
     * 产出 1 个的概率。
     * 单位：百分比。
     */
    private BigDecimal quantityChance1;

    /**
     * 产出 2 个的概率。
     * 单位：百分比。
     */
    private BigDecimal quantityChance2;

    /**
     * 产出 3 个的概率。
     * 单位：百分比。
     */
    private BigDecimal quantityChance3;

    /**
     * 判断当前是否带有装备影响快照。
     *
     * @return true-有，false-无
     */
    public boolean hasEquipmentEffectSnapshot() {
        return equipmentEffectSnapshotJson != null && !equipmentEffectSnapshotJson.trim().isEmpty();
    }

    /**
     * 判断当前是否已经锁定了奖励配置。
     *
     * @return true-已锁定，false-未锁定
     */
    public boolean hasLockedRewardConfig() {
        boolean hasSingleItem = itemCode != null && !itemCode.trim().isEmpty();
        boolean hasRewardList = rewardListJson != null && !rewardListJson.trim().isEmpty();
        return hasSingleItem || hasRewardList;
    }
}