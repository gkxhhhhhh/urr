package com.urr.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.urr.domain.wallet.WalletEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WalletMapper extends BaseMapper<WalletEntity> {
}
