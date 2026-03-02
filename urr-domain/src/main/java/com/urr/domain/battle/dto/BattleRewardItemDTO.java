package com.urr.domain.battle.dto;

import lombok.Data;

@Data
public class BattleRewardItemDTO {
    private String type;
    private String code;
    private String name;
    private Long amount;
}
