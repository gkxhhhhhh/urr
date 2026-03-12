package com.urr.api.game.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 采集写接口请求。
 *
 * 说明：
 * 1. 统一承载“立即开始 / 加入队列”两种写操作。
 * 2. mode=START_NOW 表示立即开始。
 * 3. mode=ENQUEUE 表示加入队列。
 */
@Data
public class OperateGatherTaskRequest {

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

    /**
     * 目标次数。
     * -1 表示无限次数。
     */
    @NotNull(message = "targetCount不能为空")
    private Long targetCount;

    /**
     * 操作模式。
     * START_NOW / ENQUEUE
     */
    @NotBlank(message = "mode不能为空")
    private String mode;
}