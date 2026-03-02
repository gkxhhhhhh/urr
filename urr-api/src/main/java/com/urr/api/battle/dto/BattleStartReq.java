package com.urr.api.battle.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class BattleStartReq {

    @NotNull(message = "playerId不能为空")
    private Long playerId;

    @NotBlank(message = "actionCode不能为空")
    private String actionCode;
}
