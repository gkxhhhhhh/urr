package com.urr.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.urr.domain.item.PlayerEquipEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 玩家装备实例 Mapper。
 */
@Mapper
public interface PlayerEquipMapper extends BaseMapper<PlayerEquipEntity> {
}
