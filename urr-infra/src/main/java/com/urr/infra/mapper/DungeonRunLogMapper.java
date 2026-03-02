package com.urr.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.urr.domain.battle.DungeonRunLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DungeonRunLogMapper extends BaseMapper<DungeonRunLogEntity> {
}
