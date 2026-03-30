package com.urr.app.action.task;

import com.urr.app.action.task.query.QueryGatherTaskPanelQuery;
import com.urr.app.action.task.result.QueryGatherTaskPanelResult;

/**
 * 通用职业动作读服务。
 */
public interface ProfessionActionReadService {

    /**
     * 查询职业动作面板。
     *
     * @param query 查询参数
     * @return 面板结果
     */
    QueryGatherTaskPanelResult queryPanel(QueryGatherTaskPanelQuery query);
}