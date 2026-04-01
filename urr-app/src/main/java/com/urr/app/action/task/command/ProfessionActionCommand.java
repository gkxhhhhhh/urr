package com.urr.app.action.task.command;

import lombok.Data;

/**
 * 职业动作命令。
 *
 * 说明：
 * 1. 这是 app 层命令对象，不直接承载 API 语义；
 * 2. 采集 / 制造共用同一收口；
 * 3. 强化动作会额外透传装备实例与成功率上下文。
 */
@Data
public class ProfessionActionCommand {

    /**
     * 账号ID。
     */
    private Long accountId;

    /**
     * 角色ID。
     */
    private Long playerId;

    /**
     * 命令类型。
     * START_NOW / ENQUEUE / STOP / REFRESH
     */
    private String commandType;

    /**
     * 动作编码。
     */
    private String actionCode;

    /**
     * 目标次数。
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
