package com.urr.app.game;

import com.urr.app.game.result.HomeInitSnapshotResult;

/**
 * 首页初始化快照应用服务。
 *
 * 说明：
 * 1. 只负责首页进入时的一次性聚合读取。
 * 2. 不处理动作开始/停止/入队，不处理市场交易，不处理战斗结算。
 */
public interface HomeInitSnapshotAppService {

    /**
     * 加载首页初始化快照。
     *
     * @param accountId 账号ID
     * @param playerId 角色ID
     * @return 首页初始化快照
     */
    HomeInitSnapshotResult loadSnapshot(Long accountId, Long playerId);
}