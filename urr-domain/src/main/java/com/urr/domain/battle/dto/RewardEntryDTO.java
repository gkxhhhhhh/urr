package com.urr.domain.battle.dto;

import lombok.Data;

@Data
public class RewardEntryDTO {
    private String type;
    private String currency;
    private String itemCode;
    private Integer min;
    private Integer max;
    private Integer weight;
    private Integer probPerMillion;
}
