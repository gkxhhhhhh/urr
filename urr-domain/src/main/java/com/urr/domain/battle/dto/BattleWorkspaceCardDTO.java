package com.urr.domain.battle.dto;

import lombok.Data;

/**
 * 战斗工作区卡片。
 */
@Data
public class BattleWorkspaceCardDTO {

    /**
     * 卡片编码。
     */
    private String code;

    /**
     * 卡片名称。
     */
    private String name;

    /**
     * 动作类型。
     */
    private String actionKind;

    /**
     * 最低等级。
     */
    private Integer minLevel;

    /**
     * 体力消耗。
     */
    private Integer costStamina;

    /**
     * 是否选中。
     */
    private Boolean selected;

    /**
     * 是否禁用。
     */
    private Boolean disabled;
}