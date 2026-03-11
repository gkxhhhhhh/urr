package com.urr.domain.action.task;

/**
 * 动作任务相关常量。
 */
public final class ActionTaskConstants {

    /**
     * 无限目标轮次。
     * -1 表示无限次数任务。
     */
    public static final long INFINITE_TARGET_COUNT = -1L;

    /**
     * 默认分段大小。
     * 当前采集按 10 次一段运行。
     */
    public static final int DEFAULT_SEGMENT_SIZE = 10;

    /**
     * 默认离线挂机小时数。
     */
    public static final int DEFAULT_OFFLINE_HOURS = 10;

    /**
     * 离线挂机最大小时数。
     */
    public static final int MAX_OFFLINE_HOURS = 24;

    /**
     * 单角色动作队列上限。
     * 当前会话先固定为 10，后续可再配置化。
     */
    public static final int MAX_QUEUE_SIZE_PER_PLAYER = 10;

    /**
     * 工具类不允许实例化。
     */
    private ActionTaskConstants() {
    }
}