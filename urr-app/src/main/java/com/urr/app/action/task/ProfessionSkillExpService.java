package com.urr.app.action.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.urr.domain.skill.PlayerSkillEntity;
import com.urr.domain.skill.SkillDefEntity;
import com.urr.infra.mapper.PlayerSkillMapper;
import com.urr.infra.mapper.SkillDefMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 通用职业技能经验服务。
 */
@Service
@RequiredArgsConstructor
public class ProfessionSkillExpService {

    /**
     * 技能经验曲线指数。
     */
    private static final double SKILL_EXP_POWER = 2.2D;

    /**
     * 91 级系数关键点。
     */
    private static final double COEFFICIENT_LEVEL_91 = 218D;

    /**
     * 120 级系数关键点。
     */
    private static final double COEFFICIENT_LEVEL_120 = 1585D;

    /**
     * 141 级系数关键点。
     */
    private static final double COEFFICIENT_LEVEL_141 = 10816D;

    /**
     * 167 级系数关键点。
     */
    private static final double COEFFICIENT_LEVEL_167 = 81346D;

    /**
     * 技能定义 Mapper。
     */
    private final SkillDefMapper skillDefMapper;

    /**
     * 玩家技能 Mapper。
     */
    private final PlayerSkillMapper playerSkillMapper;

    /**
     * 把职业经验正式入账。
     *
     * @param playerId 玩家ID
     * @param skillCode 技能编码
     * @param addExp 本次增加经验
     */
    public void applySkillExp(Long playerId, String skillCode, long addExp) {
        validateInput(playerId, skillCode, addExp);
        SkillDefEntity skillDef = requireSkillDef(skillCode);
        PlayerSkillEntity playerSkill = findPlayerSkill(playerId, skillDef.getId());
        long totalExp = addExp;
        if (playerSkill != null && playerSkill.getSkillExp() != null && playerSkill.getSkillExp() > 0L) {
            totalExp = playerSkill.getSkillExp() + addExp;
        }
        int newLevel = calculateSkillLevel(skillDef, totalExp);
        if (playerSkill == null) {
            insertPlayerSkill(playerId, skillDef, totalExp, newLevel);
            return;
        }
        playerSkill.setSkillExp(totalExp);
        playerSkill.setSkillLevel(newLevel);
        playerSkillMapper.updateById(playerSkill);
    }

    /**
     * 查询技能定义。
     *
     * @param skillCode 技能编码
     * @return 技能定义
     */
    private SkillDefEntity requireSkillDef(String skillCode) {
        SkillDefEntity skillDef = skillDefMapper.selectOne(
                new LambdaQueryWrapper<SkillDefEntity>()
                        .eq(SkillDefEntity::getSkillCode, skillCode)
                        .eq(SkillDefEntity::getDeleteFlag, 0)
                        .last("limit 1")
        );
        if (skillDef == null) {
            throw new IllegalStateException("技能定义不存在，skillCode=" + skillCode);
        }
        return skillDef;
    }

    /**
     * 查询玩家技能。
     *
     * @param playerId 玩家ID
     * @param skillId 技能ID
     * @return 玩家技能
     */
    private PlayerSkillEntity findPlayerSkill(Long playerId, Long skillId) {
        return playerSkillMapper.selectOne(
                new LambdaQueryWrapper<PlayerSkillEntity>()
                        .eq(PlayerSkillEntity::getPlayerId, playerId)
                        .eq(PlayerSkillEntity::getSkillId, skillId)
                        .eq(PlayerSkillEntity::getDeleteFlag, 0)
                        .last("limit 1")
        );
    }

    /**
     * 插入玩家技能记录。
     *
     * @param playerId 玩家ID
     * @param skillDef 技能定义
     * @param totalExp 总经验
     * @param level 等级
     */
    private void insertPlayerSkill(Long playerId, SkillDefEntity skillDef, long totalExp, int level) {
        PlayerSkillEntity playerSkill = new PlayerSkillEntity();
        playerSkill.setPlayerId(playerId);
        playerSkill.setSkillId(skillDef.getId());
        playerSkill.setSkillLevel(level);
        playerSkill.setSkillExp(totalExp);
        playerSkillMapper.insert(playerSkill);
    }

    /**
     * 计算技能等级。
     *
     * @param skillDef 技能定义
     * @param totalExp 总经验
     * @return 等级
     */
    private int calculateSkillLevel(SkillDefEntity skillDef, long totalExp) {
        long safeTotalExp = Math.max(totalExp, 0L);
        int maxLevel = resolveMaxLevel(skillDef);
        int currentLevel = 1;
        for (int level = 2; level <= maxLevel; level++) {
            long levelNeedTotalExp = calculateTotalExp(level);
            if (safeTotalExp < levelNeedTotalExp) {
                break;
            }
            currentLevel = level;
        }
        return currentLevel;
    }

    /**
     * 解析最大等级。
     *
     * @param skillDef 技能定义
     * @return 最大等级
     */
    private int resolveMaxLevel(SkillDefEntity skillDef) {
        if (skillDef == null || skillDef.getMaxLevel() == null || skillDef.getMaxLevel() <= 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(skillDef.getMaxLevel(), 1);
    }

    /**
     * 计算某等级累计经验。
     *
     * @param level 等级
     * @return 累计经验
     */
    private long calculateTotalExp(int level) {
        int safeLevel = Math.max(level, 1);
        double coefficient = resolveCoefficient(safeLevel);
        double totalExp = coefficient * Math.pow(safeLevel, SKILL_EXP_POWER);
        return Math.max(Math.round(totalExp), 0L);
    }

    /**
     * 解析等级系数。
     *
     * @param level 等级
     * @return 系数
     */
    private double resolveCoefficient(int level) {
        if (level <= 20) {
            return 4D;
        }
        if (level <= 23) {
            return interpolateLinear(level, 20, 4D, 23, 5D);
        }
        if (level <= 67) {
            return interpolateLinear(level, 24, 5D, 67, 48D);
        }
        if (level <= 74) {
            return interpolateLinear(level, 68, 50D, 74, 74D);
        }
        if (level <= 90) {
            return interpolateQuadratic(level, 75, 78D, 90, 210D, 91, COEFFICIENT_LEVEL_91);
        }
        if (level == 91) {
            return COEFFICIENT_LEVEL_91;
        }
        if (level <= 119) {
            return interpolateExponential(level, 92, 230D, 119, 1460D);
        }
        if (level == 120) {
            return COEFFICIENT_LEVEL_120;
        }
        if (level <= 140) {
            return interpolateExponential(level, 121, 1720D, 140, 9300D);
        }
        if (level == 141) {
            return COEFFICIENT_LEVEL_141;
        }
        if (level <= 166) {
            return interpolateExponential(level, 142, 11700D, 166, 75000D);
        }
        if (level == 167) {
            return COEFFICIENT_LEVEL_167;
        }
        return COEFFICIENT_LEVEL_167 * Math.pow(1.08D, level - 167D);
    }

    /**
     * 线性插值。
     *
     * @param level 当前等级
     * @param startLevel 起始等级
     * @param startValue 起始值
     * @param endLevel 结束等级
     * @param endValue 结束值
     * @return 插值结果
     */
    private double interpolateLinear(int level, int startLevel, double startValue, int endLevel, double endValue) {
        if (endLevel <= startLevel) {
            return startValue;
        }
        double ratio = (level - startLevel) * 1D / (endLevel - startLevel);
        return startValue + (endValue - startValue) * ratio;
    }

    /**
     * 指数插值。
     *
     * @param level 当前等级
     * @param startLevel 起始等级
     * @param startValue 起始值
     * @param endLevel 结束等级
     * @param endValue 结束值
     * @return 插值结果
     */
    private double interpolateExponential(int level, int startLevel, double startValue, int endLevel, double endValue) {
        if (endLevel <= startLevel) {
            return startValue;
        }
        if (startValue <= 0D || endValue <= 0D) {
            return startValue;
        }
        double ratio = (level - startLevel) * 1D / (endLevel - startLevel);
        return startValue * Math.pow(endValue / startValue, ratio);
    }

    /**
     * 二次插值。
     *
     * @param level 当前等级
     * @param x1 锚点1等级
     * @param y1 锚点1值
     * @param x2 锚点2等级
     * @param y2 锚点2值
     * @param x3 锚点3等级
     * @param y3 锚点3值
     * @return 插值结果
     */
    private double interpolateQuadratic(int level, int x1, double y1, int x2, double y2, int x3, double y3) {
        double current = level;
        double l1 = ((current - x2) * (current - x3)) / ((x1 - x2) * 1D * (x1 - x3));
        double l2 = ((current - x1) * (current - x3)) / ((x2 - x1) * 1D * (x2 - x3));
        double l3 = ((current - x1) * (current - x2)) / ((x3 - x1) * 1D * (x3 - x2));
        return y1 * l1 + y2 * l2 + y3 * l3;
    }

    /**
     * 校验入参。
     *
     * @param playerId 玩家ID
     * @param skillCode 技能编码
     * @param addExp 增加经验
     */
    private void validateInput(Long playerId, String skillCode, long addExp) {
        if (playerId == null) {
            throw new IllegalArgumentException("playerId不能为空");
        }
        if (!StringUtils.hasText(skillCode)) {
            throw new IllegalArgumentException("skillCode不能为空");
        }
        if (addExp <= 0L) {
            throw new IllegalArgumentException("addExp必须大于0");
        }
    }
}