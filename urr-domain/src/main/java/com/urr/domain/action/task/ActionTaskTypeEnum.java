package com.urr.domain.action.task;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 动作任务类型。
 *
 * 说明：
 * 1. 这里表达的是“任务业务类型”，例如采集/战斗/制造。
 * 2. 这里不复用 ActionDefEntity.actionKind。
 * 3. actionKind 在当前仓库里已经有固定语义：LOOP / INSTANT / MODULE。
 */
@Getter
@RequiredArgsConstructor
public enum ActionTaskTypeEnum {

    /**
     * 采集任务。
     */
    GATHER("GATHER", "采集"),

    /**
     * 战斗任务。
     */
    BATTLE("BATTLE", "战斗"),

    /**
     * 制造任务。
     */
    CRAFT("CRAFT", "制造");

    /**
     * 枚举编码。
     */
    private final String code;

    /**
     * 枚举说明。
     */
    private final String desc;
}