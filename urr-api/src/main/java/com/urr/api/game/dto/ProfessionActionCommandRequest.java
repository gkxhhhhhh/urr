package com.urr.api.game.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 职业动作命令请求。
 *
 * 说明：
 * 1. 统一承载 START_NOW / ENQUEUE / STOP / REFRESH。
 * 2. START_NOW / ENQUEUE 需要 actionCode 和 targetCount。
 * 3. STOP / REFRESH 只需要 playerId。
 */
@Data
public class ProfessionActionCommandRequest {

    /**
     * 角色ID。
     */
    @NotNull(message = "playerId不能为空")
    private Long playerId;

    /**
     * 命令类型。
     * START_NOW / ENQUEUE / STOP / REFRESH
     */
    @NotBlank(message = "commandType不能为空")
    private String commandType;

    /**
     * 动作编码。
     * START_NOW / ENQUEUE 时必填。
     */
    private String actionCode;

    /**
     * 目标次数。
     * -1 表示无限次数。
     * START_NOW / ENQUEUE 时必填。
     */
    private Long targetCount;
}