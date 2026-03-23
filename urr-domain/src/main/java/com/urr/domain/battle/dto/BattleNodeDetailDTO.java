package com.urr.domain.battle.dto;

import lombok.Data;

/**
 * 战斗节点详情。
 */
@Data
public class BattleNodeDetailDTO {

    /**
     * 角色ID。
     */
    private Long playerId;

    /**
     * 动作编码。
     */
    private String actionCode;

    /**
     * 副本编码。
     */
    private String dungeonCode;

    /**
     * 遭遇编码。
     */
    private String encounterCode;

    /**
     * 节点名称。
     */
    private String name;

    /**
     * 最低等级。
     */
    private Integer minLevel;

    /**
     * 体力消耗。
     */
    private Integer energyCost;

    /**
     * 波次数。
     */
    private Integer waveCount;

    /**
     * 是否可挑战。
     */
    private Boolean challengeable;

    /**
     * 当前是否禁用。
     */
    private Boolean disabled;

    /**
     * 玩家当前体力。
     */
    private Integer currentEnergy;

    /**
     * 当前玩家等级。
     */
    private Integer currentLevel;

    /**
     * 最佳通关阶段。
     */
    private Integer bestStage;

    /**
     * 累计挑战次数。
     */
    private Long totalRuns;
}