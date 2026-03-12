package com.urr.api.game.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 查询采集面板请求。
 */
@Data
public class QueryGatherTaskPanelRequest {

    /**
     * 角色ID。
     */
    @NotNull(message = "playerId不能为空")
    private Long playerId;
}