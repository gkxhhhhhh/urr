package com.urr.app.action.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urr.domain.action.task.PlayerGatherTask;
import com.urr.domain.skill.PlayerSkillEntity;
import com.urr.domain.skill.SkillDefEntity;
import com.urr.infra.mapper.PlayerSkillMapper;
import com.urr.infra.mapper.SkillDefMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * 采集技能经验服务。
 *
 * 说明：
 * 1. 经验在收益 flush 成功后正式入库。
 * 2. 本次等级计算规则不再写死在 Java 里，而是读取 t_urr_skill_def.meta_json。
 * 3. 当前只实现最小闭环：按 levelExpPerLevel 做线性升级。
 */
@Service
@RequiredArgsConstructor
public class GatherTaskSkillExpService {

    /**
     * 技能定义 Mapper。
     */
    private final SkillDefMapper skillDefMapper;

    /**
     * 玩家技能 Mapper。
     */
    private final PlayerSkillMapper playerSkillMapper;

    /**
     * 采集奖励生成器。
     */
    private final GatherTaskRewardGenerator gatherTaskRewardGenerator;

    /**
     * JSON 编解码器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 把 flush 成功的轮次经验正式入账。
     *
     * @param task 采集任务
     * @param flushedRoundCount 本次正式 flush 的轮次数
     */
    public void applySkillExp(PlayerGatherTask task, long flushedRoundCount) {
        validateInput(task, flushedRoundCount);

        String skillCode = gatherTaskRewardGenerator.resolveSkillCode(task);
        int expGain = gatherTaskRewardGenerator.resolveExpGain(task);
        if (!StringUtils.hasText(skillCode) || expGain <= 0) {
            return;
        }

        SkillDefEntity skillDef = requireSkillDef(skillCode);
        PlayerSkillEntity playerSkill = findPlayerSkill(task.getPlayerId(), skillDef.getId());

        long addExp = flushedRoundCount * (long) expGain;
        long totalExp = addExp;
        if (playerSkill != null && playerSkill.getSkillExp() != null && playerSkill.getSkillExp() > 0L) {
            totalExp = playerSkill.getSkillExp() + addExp;
        }

        int newLevel = calculateSkillLevel(skillDef, totalExp);
        if (playerSkill == null) {
            insertPlayerSkill(task, skillDef, totalExp, newLevel);
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
     * 查询玩家技能记录。
     *
     * @param playerId 玩家ID
     * @param skillId 技能ID
     * @return 玩家技能记录
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
     * 新增玩家技能记录。
     *
     * @param task 采集任务
     * @param skillDef 技能定义
     * @param totalExp 累计经验
     * @param level 当前等级
     */
    private void insertPlayerSkill(PlayerGatherTask task,
                                   SkillDefEntity skillDef,
                                   long totalExp,
                                   int level) {
        PlayerSkillEntity playerSkill = new PlayerSkillEntity();
        playerSkill.setPlayerId(task.getPlayerId());
        playerSkill.setServerId(task.getServerId());
        playerSkill.setSkillId(skillDef.getId());
        playerSkill.setSkillLevel(level);
        playerSkill.setSkillExp(totalExp);
        playerSkillMapper.insert(playerSkill);
    }

    /**
     * 计算技能等级。
     *
     * 规则：
     * 1. levelExpPerLevel 表示“每升一级所需经验”。
     * 2. 等级从 1 开始。
     * 3. 等级不会超过 skill_def.max_level。
     *
     * @param skillDef 技能定义
     * @param totalExp 总经验
     * @return 当前等级
     */
    private int calculateSkillLevel(SkillDefEntity skillDef, long totalExp) {
        int levelExpPerLevel = readLevelExpPerLevel(skillDef);
        int maxLevel = skillDef.getMaxLevel() == null || skillDef.getMaxLevel() <= 0
                ? Integer.MAX_VALUE
                : skillDef.getMaxLevel();

        long extraLevel = totalExp / levelExpPerLevel;
        long level = 1L + extraLevel;
        if (level > maxLevel) {
            level = maxLevel;
        }
        if (level < 1L) {
            level = 1L;
        }
        return (int) level;
    }

    /**
     * 从技能 meta_json 读取每级经验。
     *
     * @param skillDef 技能定义
     * @return 每级经验
     */
    private int readLevelExpPerLevel(SkillDefEntity skillDef) {
        if (skillDef == null || !StringUtils.hasText(skillDef.getMetaJson())) {
            throw new IllegalStateException("技能缺少 meta_json 等级配置，skillCode="
                    + (skillDef == null ? null : skillDef.getSkillCode()));
        }

        try {
            JsonNode root = objectMapper.readTree(skillDef.getMetaJson());
            JsonNode valueNode = root.get("levelExpPerLevel");
            if (valueNode == null || valueNode.isNull()) {
                throw new IllegalStateException("技能 meta_json 缺少 levelExpPerLevel，skillCode="
                        + skillDef.getSkillCode());
            }
            int value = valueNode.asInt();
            if (value <= 0) {
                throw new IllegalStateException("技能 levelExpPerLevel 必须大于0，skillCode="
                        + skillDef.getSkillCode());
            }
            return value;
        } catch (IOException e) {
            throw new IllegalStateException("技能 meta_json 解析失败，skillCode=" + skillDef.getSkillCode(), e);
        }
    }

    /**
     * 校验入参。
     *
     * @param task 采集任务
     * @param flushedRoundCount 本次刷入轮次
     */
    private void validateInput(PlayerGatherTask task, long flushedRoundCount) {
        if (task == null) {
            throw new IllegalArgumentException("采集任务不能为空");
        }
        if (task.getPlayerId() == null) {
            throw new IllegalArgumentException("采集任务 playerId 不能为空");
        }
        if (flushedRoundCount <= 0L) {
            throw new IllegalArgumentException("flushedRoundCount 必须大于0");
        }
    }
}
