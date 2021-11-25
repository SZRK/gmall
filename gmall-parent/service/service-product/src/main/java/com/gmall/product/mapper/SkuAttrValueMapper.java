package com.gmall.product.mapper;

import com.gmall.model.product.SkuAttrValue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SkuAttrValueMapper extends BaseMapper<SkuAttrValue> {


    // 根据库存信息查询销售属性和销售属性值
    List<SkuAttrValue> getAttrAndAttrValueByskuId(Long skuId);
}
