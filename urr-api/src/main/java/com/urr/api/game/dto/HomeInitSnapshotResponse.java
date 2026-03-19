package com.urr.api.game.dto;

import com.urr.domain.action.dto.ActionTreeResponseDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 首页初始化快照响应。
 *
 * 说明：
 * 1. 只返回首页初始化真实需要的稳定字段。
 * 2. 页面骨架、市场交易、战斗结算等不进入这里。
 */
@Data
public class HomeInitSnapshotResponse {

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
    private List<SkillItem> skills;

    /**
     * 当前首页任务面板。
     */
    private GatherTaskPanelResponse panel;

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
        private LocalDateTime energyUpdateTime;
        private LocalDateTime lastOnlineTime;
        private LocalDateTime lastInteractTime;
        private LocalDateTime lastSettleTime;
    }

    /**
     * 首页技能项。
     */
    @Data
    public static class SkillItem {

        private String code;
        private String name;
        private String type;
        private Integer level;
        private Integer progress;
        private Integer extraLevel;
    }
}