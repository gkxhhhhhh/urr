package com.urr.api.battle;

import com.urr.api.auth.AuthRequired;
import com.urr.api.battle.dto.BattleNodeDetailQueryReq;
import com.urr.api.battle.dto.BattleStartReq;
import com.urr.api.battle.dto.BattleWorkspaceQueryReq;
import com.urr.app.battle.BattleAppService;
import com.urr.commons.api.ResponseData;
import com.urr.domain.battle.dto.BattleNodeDetailDTO;
import com.urr.domain.battle.dto.BattleStartResultDTO;
import com.urr.domain.battle.dto.BattleWorkspaceDTO;
import com.urr.domain.battle.dto.LastBattleResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 战斗接口 Controller。
 *
 * 说明：
 * 1. 只处理战斗页自己的查询与命令。
 * 2. 不回收首页快照、市场、职业动作等已拆分模块。
 */
@RestController
@RequestMapping("/api/battle")
@RequiredArgsConstructor
@Validated
public class BattleController {

    /**
     * 战斗应用服务。
     */
    private final BattleAppService battleAppService;

    /**
     * 查询战斗首页轻量工作区数据。
     *
     * @param req 查询请求
     * @return 战斗工作区轻量快照
     */
    @GetMapping("/workspace")
    public ResponseData<BattleWorkspaceDTO> workspace(@Valid BattleWorkspaceQueryReq req) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(
                battleAppService.loadWorkspace(accountId, req.getPlayerId())
        );
    }

    /**
     * 查询单个战斗节点详情。
     *
     * @param req 查询请求
     * @return 节点详情
     */
    @GetMapping("/node/detail")
    public ResponseData<BattleNodeDetailDTO> nodeDetail(@Valid BattleNodeDetailQueryReq req) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(
                battleAppService.loadNodeDetail(accountId, req.getPlayerId(), req.getActionCode())
        );
    }

    /**
     * 查询最近一次战斗结果。
     *
     * @param playerId 角色ID
     * @return 最近一次战斗结果
     */
    @GetMapping("/last-result")
    public ResponseData<LastBattleResultDTO> lastResult(@RequestParam("playerId") Long playerId) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(
                battleAppService.queryLastBattleResult(accountId, playerId)
        );
    }

    /**
     * 发起一次战斗。
     *
     * @param req 战斗启动请求
     * @return 战斗结果
     */
    @PostMapping("/start")
    public ResponseData<BattleStartResultDTO> start(@RequestBody @Valid BattleStartReq req) {
        Long accountId = AuthRequired.requireAccountId();
        return ResponseData.success(
                battleAppService.startBattle(accountId, req.getPlayerId(), req.getActionCode())
        );
    }
}