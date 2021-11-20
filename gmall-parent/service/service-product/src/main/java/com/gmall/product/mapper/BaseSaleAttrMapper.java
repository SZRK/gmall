package com.gmall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gmall.model.product.BaseSaleAttr;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface BaseSaleAttrMapper extends BaseMapper<BaseSaleAttr> {
}
