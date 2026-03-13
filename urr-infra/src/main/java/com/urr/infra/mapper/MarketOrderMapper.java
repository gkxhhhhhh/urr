package com.urr.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.urr.domain.market.MarketOrderEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 市场订单 Mapper。
 */
@Mapper
public interface MarketOrderMapper extends BaseMapper<MarketOrderEntity> {
}