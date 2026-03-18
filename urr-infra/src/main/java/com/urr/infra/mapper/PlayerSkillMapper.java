package com.urr.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.urr.domain.skill.PlayerSkillEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 玩家技能 Mapper。
 */
@Mapper
public interface PlayerSkillMapper extends BaseMapper<PlayerSkillEntity> {
}