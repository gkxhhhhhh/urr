package com.urr.api.battle.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 战斗节点详情查询请求。
 */
@Data
public class BattleNodeDetailQueryReq {

    /**
     * 角色ID。
     */
    @NotNull(message = "playerId不能为空")
    private Long playerId;

    /**
     * 动作编码。
     */
    @NotBlank(message = "actionCode不能为空")
    private String actionCode;
}