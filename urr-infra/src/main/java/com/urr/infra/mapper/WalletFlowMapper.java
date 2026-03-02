package com.urr.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.urr.domain.wallet.WalletFlowEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WalletFlowMapper extends BaseMapper<WalletFlowEntity> {
}
