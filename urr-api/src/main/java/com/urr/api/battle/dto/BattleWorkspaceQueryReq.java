package com.urr.api.battle.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 战斗工作区查询请求。
 */
@Data
public class BattleWorkspaceQueryReq {

    /**
     * 角色ID。
     */
    @NotNull(message = "playerId不能为空")
    private Long playerId;
}