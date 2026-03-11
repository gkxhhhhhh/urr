package com.urr.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.urr.domain.action.task.PlayerActionQueueEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 玩家动作队列 Mapper。
 */
@Mapper
public interface PlayerActionQueueMapper extends BaseMapper<PlayerActionQueueEntity> {
}