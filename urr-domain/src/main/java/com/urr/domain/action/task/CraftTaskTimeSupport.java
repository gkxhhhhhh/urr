package com.urr.domain.action.task;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 制造任务时间工具。
 */
public final class CraftTaskTimeSupport {

    /**
     * 工具类不允许实例化。
     */
    private CraftTaskTimeSupport() {
    }

    /**
     * 把 LocalDateTime 转成毫秒时间戳。
     *
     * @param time 时间
     * @return 毫秒时间戳
     */
    public static long toEpochMilli(LocalDateTime time) {
        if (time == null) {
            return 0L;
        }
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 把毫秒时间戳转成 LocalDateTime。
     *
     * @param epochMilli 毫秒时间戳
     * @return 时间对象
     */
    public static LocalDateTime fromEpochMilli(Long epochMilli) {
        if (epochMilli == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault());
    }
}