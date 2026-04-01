package com.urr.api.game.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 职业动作命令请求。
 *
 * 说明：
 * 1. 统一承载 START_NOW / ENQUEUE / STOP / REFRESH；
 * 2. START_NOW / ENQUEUE 需要 actionCode 和 targetCount；
 * 3. 强化类动作额外透传装备实例与成功率上下文；
 * 4. STOP / REFRESH 只需要 playerId。
 */
@Data
public class ProfessionActionCommandRequest {

    /**
     * 角色ID。
     */
    @NotNull(message = "playerId不能为空")
    private Long playerId;

    /**
     * 命令类型。
     * START_NOW / ENQUEUE / STOP / REFRESH
     */
    @NotBlank(message = "commandType不能为空")
    private String commandType;

    /**
     * 动作编码。
     * START_NOW / ENQUEUE 时必填。
     */
    private String actionCode;

    /**
     * 目标次数。
     * -1 表示无限次数。
     * START_NOW / ENQUEUE 时必填。
     */
    private Long targetCount;

    /**
     * 强化装备实例ID。
     */
    private Long equipInstanceId;

    /**
     * 强化茶类型。
     * NONE / NORMAL / SUPER / ULTRA
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
     * 例如 0.05 表示 +5%。
     */
    private Double extraSuccessRate;
}
