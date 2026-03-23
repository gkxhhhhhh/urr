package com.urr.domain.battle.dto;

import lombok.Data;

/**
 * 战斗奖励项。
 *
 * 说明：
 * 1. 保留 code / name，兼容当前后端已有结算逻辑。
 * 2. 增加 currency / itemCode，兼容当前前端战斗奖励展示字段。
 */
@Data
public class BattleRewardItemDTO {

    /**
     * 奖励类型：currency / item。
     */
    private String type;

    /**
     * 货币编码。
     */
    private String currency;

    /**
     * 物品编码。
     */
    private String itemCode;

    /**
     * 兼容字段：统一奖励编码。
     */
    private String code;

    /**
     * 奖励名称。
     */
    private String name;

    /**
     * 奖励数量。
     */
    private Long amount;
}