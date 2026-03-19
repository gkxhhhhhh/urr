package com.urr.app.game.result;

import com.urr.app.action.task.result.QueryGatherTaskPanelResult;
import com.urr.domain.action.dto.ActionTreeResponseDTO;
import lombok.Data;

import java.util.List;

/**
 * 首页初始化快照 app 层结果。
 *
 * 说明：
 * 1. 这里只承载首页进入时真正需要的一次性聚合结果。
 * 2. 不把动作执行、市场交易、战斗结算塞进来。
 */
@Data
public class HomeInitSnapshotResult {

    /**
     * 当前角色基础信息。
     */
    private PlayerSummary player;

    /**
     * 当前区服活跃角色数。
     */
    private Integer activeRoleCount;

    /**
     * 首页左侧导航树。
     */
    private ActionTreeResponseDTO leftNav;

    /**
     * 职业/技能摘要。
     */
    private List<SkillSummary> skills;

    /**
     * 当前首页任务面板数据。
     *
     * 说明：
     * 当前直接复用采集任务面板读结果，覆盖首页任务区、队列、待领奖励、展示背包等真实依赖。
     */
    private QueryGatherTaskPanelResult panel;

    /**
     * 当前角色基础信息。
     */
    @Data
    public static class PlayerSummary {

        private Long playerId;
        private Integer serverId;
        private Integer type;
        private String nickname;
        private String avatar;
        private Integer level;
        private Long exp;
        private Long power;
        private Integer energy;
        private java.time.LocalDateTime energyUpdateTime;
        private java.time.LocalDateTime lastOnlineTime;
        private java.time.LocalDateTime lastInteractTime;
        private java.time.LocalDateTime lastSettleTime;
    }

    /**
     * 首页技能摘要。
     */
    @Data
    public static class SkillSummary {

        private String code;
        private String name;
        private String type;
        private Integer level;
        private Integer progress;
        private Integer extraLevel;
    }
}