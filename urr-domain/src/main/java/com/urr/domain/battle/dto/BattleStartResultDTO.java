package com.urr.domain.battle.dto;

import lombok.Data;

import java.util.List;

@Data
public class BattleStartResultDTO {
    private Long playerId;
    private String actionCode;
    private String dungeonCode;
    private String encounterCode;
    private String result;
    private Integer energyCost;
    private Integer battleTimeMs;
    private Integer beforeEnergy;
    private Integer afterEnergy;
    private List<BattleWaveResultDTO> waves;
    private List<BattleRewardItemDTO> totalRewards;
}
