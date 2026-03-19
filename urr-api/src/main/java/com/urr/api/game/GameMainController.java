package com.urr.api.game;

import com.urr.api.auth.AuthRequired;
import com.urr.api.game.dto.HomeInitSnapshotResponse;
import com.urr.app.game.HomeInitSnapshotAppService;
import com.urr.app.game.result.HomeInitSnapshotResult;
import com.urr.commons.api.ResponseData;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页主接口 Controller。
 *
 * 说明：
 * 1. 当前只提供首页初始化快照接口。
 * 2. 不在这里处理动作写接口、市场接口、战斗接口。
 */
@RestController
@RequestMapping("/api/game/main")
@RequiredArgsConstructor
@Validated
public class GameMainController {

    /**
     * 首页初始化快照应用服务。
     */
    private final HomeInitSnapshotAppService homeInitSnapshotAppService;

    /**
     * 首页主接口组装器。
     */
    private final GameMainControllerAssembler gameMainControllerAssembler;

    /**
     * 查询首页初始化快照。
     *
     * @param playerId 角色ID
     * @return 首页初始化快照
     */
    @GetMapping("/init")
    public ResponseData<HomeInitSnapshotResponse> init(@RequestParam("playerId") Long playerId) {
        Long accountId = AuthRequired.requireAccountId();
        HomeInitSnapshotResult result = homeInitSnapshotAppService.loadSnapshot(accountId, playerId);
        HomeInitSnapshotResponse response = gameMainControllerAssembler.toHomeInitSnapshotResponse(result);
        return ResponseData.success(response);
    }
}