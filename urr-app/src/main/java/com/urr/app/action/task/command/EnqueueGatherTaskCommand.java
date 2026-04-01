package com.urr.app.action.task.command;

import lombok.Data;

/**
 * 加入采集/制造队列命令。
 */
@Data
public class EnqueueGatherTaskCommand {

    /**
     * 账号ID。
     */
    private Long accountId;

    /**
     * 玩家ID。
     */
    private Long playerId;

    /**
     * 动作编码。
     */
    private String actionCode;

    /**
     * 目标轮次。
     * -1 表示无限次数。
     */
    private Long targetCount;

    /**
     * 强化装备实例ID。
     */
    private Long equipInstanceId;

    /**
     * 强化茶类型。
     */
    private String teaType;

    /**
     * 是否使用 Blessed Tea。
     */
    private Boolean blessedTeaUsed;

    /**
     * 饮茶浓度。
     */
    private Double drinkConcentration;

    /**
     * 天文台等级。
     */
    private Integer observatoryLevel;

    /**
     * 额外成功率加成。
     */
    private Double extraSuccessRate;
}
