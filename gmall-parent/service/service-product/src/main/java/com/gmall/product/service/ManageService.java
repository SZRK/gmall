package com.gmall.product.service;

import com.gmall.model.product.BaseAttrInfo;
import com.gmall.model.product.BaseCategory1;
import com.gmall.model.product.BaseCategory2;
import com.gmall.model.product.BaseCategory3;

import java.util.List;

public interface ManageService {
    // 获取一级分类
    List<BaseCategory1> getCategory1();

    // 根据一级分类Id获取二级分类
    List<BaseCategory2> getCategory2(Long category1Id);

    // 根据二级分类Id获取三级分类
    List<BaseCategory3> getCategory3(Long category2Id);

    List<BaseAttrInfo> attrInfoList(Long category1Id, Long category2Id, Long category3Id);
}
