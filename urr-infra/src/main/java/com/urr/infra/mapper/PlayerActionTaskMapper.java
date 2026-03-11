package com.urr.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.urr.domain.action.task.PlayerActionTaskEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 动作任务根表 Mapper。
 */
@Mapper
public interface PlayerActionTaskMapper extends BaseMapper<PlayerActionTaskEntity> {
}