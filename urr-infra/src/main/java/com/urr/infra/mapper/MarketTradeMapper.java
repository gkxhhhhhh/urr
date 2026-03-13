package com.urr.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.urr.domain.market.MarketTradeEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 市场成交 Mapper。
 */
@Mapper
public interface MarketTradeMapper extends BaseMapper<MarketTradeEntity> {
}