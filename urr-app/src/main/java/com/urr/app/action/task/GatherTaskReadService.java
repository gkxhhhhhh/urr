package com.urr.app.action.task;

import com.urr.app.action.task.query.QueryGatherTaskPanelQuery;
import com.urr.app.action.task.result.QueryGatherTaskPanelResult;

/**
 * 采集任务读服务。
 *
 * 说明：
 * 1. 这里只负责“采集面板所需”的最小读能力。
 * 2. 不负责启动、停止、flush、完整恢复和自动队列消费。
 */
public interface GatherTaskReadService {

    /**
     * 查询采集面板最小视图。
     *
     * @param query 查询参数
     * @return 面板结果
     */
    QueryGatherTaskPanelResult queryPanel(QueryGatherTaskPanelQuery query);
}