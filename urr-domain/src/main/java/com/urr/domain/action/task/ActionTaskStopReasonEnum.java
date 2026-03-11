package com.urr.domain.action.task;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 动作任务停止/结束原因。
 */
@Getter
@RequiredArgsConstructor
public enum ActionTaskStopReasonEnum {

    /**
     * 玩家主动停止。
     */
    USER_STOP("USER_STOP", "玩家主动停止"),

    /**
     * 玩家启动新动作替换旧动作。
     */
    USER_REPLACE("USER_REPLACE", "玩家替换当前动作"),

    /**
     * 离线超时。
     */
    OFFLINE_TIMEOUT("OFFLINE_TIMEOUT", "离线超时"),

    /**
     * 达到目标轮次，正常完成。
     */
    FINISHED("FINISHED", "正常完成"),

    /**
     * 运行异常。
     */
    ERROR("ERROR", "运行异常");

    /**
     * 枚举编码。
     */
    private final String code;

    /**
     * 枚举说明。
     */
    private final String desc;
}