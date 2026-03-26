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

import java.util.List;

/**
 * 首页主接口 Controller。
 *
 * 说明：
 * 1. 当前只处理首页初始化所需的稳定读接口。
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

    /**
     * 查询首页技能面板。
     *
     * 说明：
     * 1. 复用现有首页初始化快照能力，避免重复写一套技能查询逻辑。
     * 2. 当前前端左侧导航等级展示依赖这个接口返回的真实 skills。
     *
     * @param playerId 角色ID
     * @return 技能面板
     */
    @GetMapping("/skills")
    public ResponseData<List<HomeInitSnapshotResponse.SkillItem>> skills(@RequestParam("playerId") Long playerId) {
        Long accountId = AuthRequired.requireAccountId();
        HomeInitSnapshotResult result = homeInitSnapshotAppService.loadSnapshot(accountId, playerId);
        HomeInitSnapshotResponse response = gameMainControllerAssembler.toHomeInitSnapshotResponse(result);
        return ResponseData.success(response.getSkills());
    }
}