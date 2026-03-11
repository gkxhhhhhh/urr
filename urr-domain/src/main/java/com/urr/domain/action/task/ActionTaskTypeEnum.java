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

    /**
     * 按编码获取枚举。
     *
     * @param code 枚举编码
     * @return 匹配到的枚举，未匹配时返回 null
     */
    public static ActionTaskTypeEnum fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        for (ActionTaskTypeEnum item : values()) {
            if (item.code.equalsIgnoreCase(code.trim())) {
                return item;
            }
        }
        return null;
    }
}