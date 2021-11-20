package com.gmall.product.mapper;


import com.gmall.model.product.SkuSaleAttrValue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gmall.model.product.SpuSaleAttrValue;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface SkuSaleAttrValueMapper extends BaseMapper<SkuSaleAttrValue> {

    //查询组合对应库存ID
    // {颜色|版本|套装 : skuId,颜色|版本|套装 : skuId,颜色|版本|套装 : skuId,颜色|版本|套装 : skuId}
    List<Map> getSkuValueIdsMap(Long spuId);

    List<SpuSaleAttrValue> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId);
}
