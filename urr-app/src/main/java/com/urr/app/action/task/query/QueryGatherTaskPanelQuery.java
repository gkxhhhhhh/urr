package com.urr.app.action.task.query;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 查询采集任务面板命令。
 *
 * 说明：
 * 1. 这里只承载“采集读接口”最小入参。
 * 2. 当前按账号 + 角色维度做归属校验。
 * 3. readTime 允许外部传入，便于后续测试和重放；为空时默认取当前时间。
 */
@Data
public class QueryGatherTaskPanelQuery {

    /**
     * 账号ID。
     */
    private Long accountId;

    /**
     * 角色ID。
     */
    private Long playerId;

    /**
     * 读取时刻。
     */
    private LocalDateTime readTime;
}