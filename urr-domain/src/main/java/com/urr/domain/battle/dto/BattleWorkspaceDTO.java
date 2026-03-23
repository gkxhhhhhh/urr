package com.urr.domain.battle.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 战斗工作区轻量快照。
 */
@Data
public class BattleWorkspaceDTO {

    /**
     * 标题。
     */
    private String title;

    /**
     * 标签列表。
     */
    private List<BattleWorkspaceTabDTO> tabs = new ArrayList<>();

    /**
     * 区域列表。
     */
    private List<BattleWorkspaceAreaDTO> areas = new ArrayList<>();

    /**
     * 战斗卡片列表。
     */
    private List<BattleWorkspaceCardDTO> cards = new ArrayList<>();
}