package com.urr.domain.battle.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 最近一次战斗结果。
 */
@Data
public class LastBattleResultDTO {

    /**
     * 玩家ID。
     */
    private Long playerId;

    /**
     * 战斗运行记录ID。
     */
    private Long runId;

    /**
     * 副本编码。
     */
    private String dungeonCode;

    /**
     * 战斗结果。
     */
    private String result;

    /**
     * 体力消耗。
     */
    private Integer energyCost;

    /**
     * 战斗耗时（毫秒）。
     */
    private Integer battleTimeMs;

    /**
     * 波次结果。
     */
    private List<BattleWaveResultDTO> waves = new ArrayList<>();

    /**
     * 总奖励。
     */
    private List<BattleRewardItemDTO> totalRewards = new ArrayList<>();
}