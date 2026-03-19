package com.urr.api.game;

import com.urr.api.game.dto.HomeInitSnapshotResponse;
import com.urr.app.game.result.HomeInitSnapshotResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页初始化快照 Controller 组装器。
 *
 * 说明：
 * 1. 这里只负责 app result -> api response 的轻量转换。
 * 2. 不承载业务规则。
 */
@Component
public class GameMainControllerAssembler {

    /**
     * 采集任务 Controller 组装器。
     */
    private final GatherTaskControllerAssembler gatherTaskControllerAssembler;

    /**
     * 构造函数。
     *
     * @param gatherTaskControllerAssembler 采集任务 Controller 组装器
     */
    public GameMainControllerAssembler(GatherTaskControllerAssembler gatherTaskControllerAssembler) {
        this.gatherTaskControllerAssembler = gatherTaskControllerAssembler;
    }

    /**
     * 转换首页初始化快照响应。
     *
     * @param result app 层结果
     * @return api 层响应
     */
    public HomeInitSnapshotResponse toHomeInitSnapshotResponse(HomeInitSnapshotResult result) {
        if (result == null) {
            return null;
        }

        HomeInitSnapshotResponse response = new HomeInitSnapshotResponse();
        response.setPlayer(toPlayerSummary(result.getPlayer()));
        response.setActiveRoleCount(result.getActiveRoleCount());
        response.setLeftNav(result.getLeftNav());
        response.setSkills(toSkillItemList(result.getSkills()));
        response.setPanel(gatherTaskControllerAssembler.toPanelResponse(result.getPanel()));
        return response;
    }

    /**
     * 转换角色基础摘要。
     *
     * @param summary app 层角色摘要
     * @return api 层角色摘要
     */
    private HomeInitSnapshotResponse.PlayerSummary toPlayerSummary(HomeInitSnapshotResult.PlayerSummary summary) {
        if (summary == null) {
            return null;
        }

        HomeInitSnapshotResponse.PlayerSummary response = new HomeInitSnapshotResponse.PlayerSummary();
        response.setPlayerId(summary.getPlayerId());
        response.setServerId(summary.getServerId());
        response.setType(summary.getType());
        response.setNickname(summary.getNickname());
        response.setAvatar(summary.getAvatar());
        response.setLevel(summary.getLevel());
        response.setExp(summary.getExp());
        response.setPower(summary.getPower());
        response.setEnergy(summary.getEnergy());
        response.setEnergyUpdateTime(summary.getEnergyUpdateTime());
        response.setLastOnlineTime(summary.getLastOnlineTime());
        response.setLastInteractTime(summary.getLastInteractTime());
        response.setLastSettleTime(summary.getLastSettleTime());
        return response;
    }

    /**
     * 转换技能摘要列表。
     *
     * @param skillList app 层技能列表
     * @return api 层技能列表
     */
    private List<HomeInitSnapshotResponse.SkillItem> toSkillItemList(List<HomeInitSnapshotResult.SkillSummary> skillList) {
        List<HomeInitSnapshotResponse.SkillItem> responseList = new ArrayList<HomeInitSnapshotResponse.SkillItem>();
        if (skillList == null || skillList.isEmpty()) {
            return responseList;
        }

        for (int i = 0; i < skillList.size(); i++) {
            HomeInitSnapshotResult.SkillSummary summary = skillList.get(i);
            HomeInitSnapshotResponse.SkillItem item = new HomeInitSnapshotResponse.SkillItem();
            item.setCode(summary.getCode());
            item.setName(summary.getName());
            item.setType(summary.getType());
            item.setLevel(summary.getLevel());
            item.setProgress(summary.getProgress());
            item.setExtraLevel(summary.getExtraLevel());
            responseList.add(item);
        }
        return responseList;
    }
}