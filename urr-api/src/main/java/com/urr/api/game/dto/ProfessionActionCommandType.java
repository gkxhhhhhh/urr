package com.urr.api.game.dto;

/**
 * 职业动作命令类型。
 *
 * 说明：
 * 1. 当前先只收口前端已落地的职业动作交互。
 * 2. 当前支持：立即开始 / 入队 / 停止 / 刷新。
 * 3. 当前不在这里硬塞 claimReward，避免脱离仓库真实能力。
 */
public enum ProfessionActionCommandType {

    /**
     * 立即开始。
     */
    START_NOW("START_NOW"),

    /**
     * 加入队列。
     */
    ENQUEUE("ENQUEUE"),

    /**
     * 停止当前任务。
     */
    STOP("STOP"),

    /**
     * 仅刷新当前任务面板。
     */
    REFRESH("REFRESH");

    /**
     * 命令编码。
     */
    private final String code;

    /**
     * 构造函数。
     *
     * @param code 命令编码
     */
    ProfessionActionCommandType(String code) {
        this.code = code;
    }

    /**
     * 获取命令编码。
     *
     * @return 命令编码
     */
    public String getCode() {
        return code;
    }

    /**
     * 按编码解析命令类型。
     *
     * @param code 命令编码
     * @return 命令类型；解析失败返回 null
     */
    public static ProfessionActionCommandType fromCode(String code) {
        if (code == null) {
            return null;
        }

        String normalized = code.trim();
        ProfessionActionCommandType[] values = ProfessionActionCommandType.values();
        for (int i = 0; i < values.length; i++) {
            ProfessionActionCommandType item = values[i];
            if (item.code.equalsIgnoreCase(normalized)) {
                return item;
            }
        }
        return null;
    }
}