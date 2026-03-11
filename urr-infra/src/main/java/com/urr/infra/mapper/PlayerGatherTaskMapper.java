package com.urr.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.urr.domain.action.task.PlayerGatherTaskEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采集任务扩展表 Mapper。
 */
@Mapper
public interface PlayerGatherTaskMapper extends BaseMapper<PlayerGatherTaskEntity> {
}