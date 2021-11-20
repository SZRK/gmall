package com.gmall.product.mapper;

import com.gmall.model.product.SkuAttrValue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SkuAttrValueMapper extends BaseMapper<SkuAttrValue> {


}
