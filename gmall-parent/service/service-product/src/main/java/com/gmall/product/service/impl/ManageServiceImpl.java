package com.gmall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gmall.model.product.BaseAttrInfo;
import com.gmall.model.product.BaseCategory1;
import com.gmall.model.product.BaseCategory2;
import com.gmall.model.product.BaseCategory3;
import com.gmall.product.mapper.BaseAttrInfoMapper;
import com.gmall.product.mapper.BaseCategory1Mapper;
import com.gmall.product.mapper.BaseCategory2Mapper;
import com.gmall.product.mapper.BaseCategory3Mapper;
import com.gmall.product.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ManageServiceImpl implements ManageService {
    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;


    // 获取一级分类
    @Override
    public List<BaseCategory1> getCategory1() {
        return baseCategory1Mapper.selectList(null);
    }

    // 根根据一级分类Id获取二级分类
    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        return baseCategory2Mapper.selectList
                (new QueryWrapper<BaseCategory2>()
                        .eq("category1_id", category1Id));
    }

    // 根根据二级分类Id获取三级分类
    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        return baseCategory3Mapper.selectList
                (new QueryWrapper<BaseCategory3>()
                        .eq("category2_id", category2Id));
    }

    @Override
    public List<BaseAttrInfo> attrInfoList(Long category1Id, Long category2Id, Long category3Id) {

        return baseAttrInfoMapper.selectBaseAttrInfoList(category1Id, category2Id, category3Id);
    }

}
