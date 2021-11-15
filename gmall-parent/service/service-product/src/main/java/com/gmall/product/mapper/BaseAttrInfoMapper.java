package com.gmall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gmall.model.product.BaseAttrInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface BaseAttrInfoMapper extends BaseMapper<BaseAttrInfo> {
    List<BaseAttrInfo> selectBaseAttrInfoList(@Param("category1Id") Long category1Id
                                              ,@Param("category2Id")Long category2Id
                                              ,@Param("category3Id")Long category3Id);
}
