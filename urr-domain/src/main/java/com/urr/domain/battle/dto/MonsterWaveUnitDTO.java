package com.urr.domain.battle.dto;

import lombok.Data;

@Data
public class MonsterWaveUnitDTO {
    private String monsterCode;
    private Integer count;
    private Integer levelOverride;
    private Integer hpMul;
    private Integer atkMul;
}
