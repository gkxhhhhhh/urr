package com.urr.api.game.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * 采集写接口操作模式。
 */
@Getter
@RequiredArgsConstructor
public enum GatherTaskOperateMode {

    /**
     * 立即开始。
     */
    START_NOW("START_NOW", "立即开始"),

    /**
     * 加入队列。
     */
    ENQUEUE("ENQUEUE", "加入队列");

    /**
     * 模式编码。
     */
    private final String code;

    /**
     * 模式说明。
     */
    private final String desc;

    /**
     * 根据编码解析模式。
     *
     * @param code 模式编码
     * @return 模式枚举
     */
    public static GatherTaskOperateMode fromCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }

        for (GatherTaskOperateMode item : values()) {
            if (item.getCode().equalsIgnoreCase(code.trim())) {
                return item;
            }
        }
        return null;
    }
}