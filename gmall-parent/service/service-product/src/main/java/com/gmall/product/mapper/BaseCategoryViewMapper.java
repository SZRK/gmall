package com.gmall.product.mapper;


import com.gmall.model.product.BaseCategoryView;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface BaseCategoryViewMapper extends BaseMapper<BaseCategoryView> {

    // 通过三级分类id查询分类信息
}
