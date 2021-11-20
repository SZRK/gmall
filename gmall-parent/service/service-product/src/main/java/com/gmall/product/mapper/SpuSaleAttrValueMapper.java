package com.gmall.product.mapper;

import com.gmall.model.product.SpuSaleAttr;
import com.gmall.model.product.SpuSaleAttrValue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SpuSaleAttrValueMapper extends BaseMapper<SpuSaleAttrValue> {
    //根据skuId 查询 平台属性ID、属性名称、及平台属性值
    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(@Param("skuId") Long skuId, @Param("spuId") Long spuId);
}
