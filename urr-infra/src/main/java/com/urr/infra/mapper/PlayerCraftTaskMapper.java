package com.urr.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.urr.domain.action.task.PlayerCraftTaskEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 玩家制造任务明细 Mapper。
 */
@Mapper
public interface PlayerCraftTaskMapper extends BaseMapper<PlayerCraftTaskEntity> {
}