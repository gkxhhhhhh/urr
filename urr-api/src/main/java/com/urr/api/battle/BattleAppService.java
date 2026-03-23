package com.urr.app.battle;

import com.urr.domain.battle.dto.BattleNodeDetailDTO;
import com.urr.domain.battle.dto.BattleStartResultDTO;
import com.urr.domain.battle.dto.BattleWorkspaceDTO;
import com.urr.domain.battle.dto.LastBattleResultDTO;

/**
 * 战斗应用服务。
 */
public interface BattleAppService {

    /**
     * 查询战斗工作区轻量快照。
     *
     * @param accountId 账号ID
     * @param playerId 角色ID
     * @return 战斗工作区
     */
    BattleWorkspaceDTO loadWorkspace(Long accountId, Long playerId);

    /**
     * 查询单个战斗节点详情。
     *
     * @param accountId 账号ID
     * @param playerId 角色ID
     * @param actionCode 动作编码
     * @return 节点详情
     */
    BattleNodeDetailDTO loadNodeDetail(Long accountId, Long playerId, String actionCode);

    /**
     * 查询最近一次战斗结果。
     *
     * @param accountId 账号ID
     * @param playerId 角色ID
     * @return 最近一次战斗结果
     */
    LastBattleResultDTO queryLastBattleResult(Long accountId, Long playerId);

    /**
     * 发起一次战斗。
     *
     * @param accountId 账号ID
     * @param playerId 角色ID
     * @param actionCode 动作编码
     * @return 战斗结果
     */
    BattleStartResultDTO startBattle(Long accountId, Long playerId, String actionCode);
}