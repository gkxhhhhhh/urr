package com.urr.domain.battle.dto;

import lombok.Data;

import java.util.List;

@Data
public class BattleWaveResultDTO {
    private Integer waveNo;
    private List<BattleRewardItemDTO> rewards;
}
