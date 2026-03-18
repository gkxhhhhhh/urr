package com.urr.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.urr.domain.skill.SkillDefEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 技能定义 Mapper。
 */
@Mapper
public interface SkillDefMapper extends BaseMapper<SkillDefEntity> {
}