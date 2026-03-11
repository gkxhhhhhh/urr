package com.urr.domain.action.task;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 动作任务状态。
 */
@Getter
@RequiredArgsConstructor
public enum ActionTaskStatusEnum {

    /**
     * 已创建但尚未真正进入运行态。
     */
    QUEUED("QUEUED", "排队中", false),

    /**
     * 运行中。
     */
    RUNNING("RUNNING", "运行中", false),

    /**
     * 正常完成。
     */
    COMPLETED("COMPLETED", "已完成", true),

    /**
     * 被主动停止。
     */
    STOPPED("STOPPED", "已停止", true),

    /**
     * 因离线超时等原因过期结束。
     */
    EXPIRED("EXPIRED", "已过期", true),

    /**
     * 运行失败。
     */
    FAILED("FAILED", "失败", true);

    /**
     * 枚举编码。
     */
    private final String code;

    /**
     * 枚举说明。
     */
    private final String desc;

    /**
     * 是否终态。
     */
    private final boolean terminal;

    /**
     * 判断当前状态是否为终态。
     *
     * @return true-终态，false-非终态
     */
    public boolean isTerminal() {
        return terminal;
    }

    /**
     * 按编码获取枚举。
     *
     * @param code 枚举编码
     * @return 匹配到的枚举，未匹配时返回 null
     */
    public static ActionTaskStatusEnum fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        for (ActionTaskStatusEnum item : values()) {
            if (item.code.equalsIgnoreCase(code.trim())) {
                return item;
            }
        }
        return null;
    }
}