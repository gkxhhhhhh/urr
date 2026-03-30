package com.urr.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.urr.domain.craft.RecipeDefEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 制造配方定义 Mapper。
 */
@Mapper
public interface RecipeDefMapper extends BaseMapper<RecipeDefEntity> {
}