package com.urr.domain.battle.dto;

import lombok.Data;

import java.util.List;

@Data
public class BattleRewardConfigDTO {
    private List<RewardEntryDTO> rolls;
    private List<RewardEntryDTO> pickOneByWeight;
}
