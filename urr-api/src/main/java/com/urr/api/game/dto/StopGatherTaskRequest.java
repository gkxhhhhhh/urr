package com.urr.api.game.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 停止当前采集任务请求。
 */
@Data
public class StopGatherTaskRequest {

    /**
     * 角色ID。
     */
    @NotNull(message = "playerId不能为空")
    private Long playerId;
}