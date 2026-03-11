package com.urr.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.urr.domain.action.CategoryDefEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryDefMapper extends BaseMapper<CategoryDefEntity> {
}