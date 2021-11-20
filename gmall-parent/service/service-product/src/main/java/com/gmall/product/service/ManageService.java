package com.gmall.product.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gmall.model.product.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ManageService {
    // 获取一级分类
    List<BaseCategory1> getCategory1();

    // 根据一级分类Id获取二级分类
    List<BaseCategory2> getCategory2(Long category1Id);

    // 根据二级分类Id获取三级分类
    List<BaseCategory3> getCategory3(Long category2Id);

    // 根据分类id获取平台属性
    List<BaseAttrInfo> attrInfoList(Long category1Id, Long category2Id, Long category3Id);

    // 添加平台属性
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    // 根据平台属性ID获取平台属性
    List<BaseAttrValue>  getAttrValueList(Long arrtId);

    IPage<BaseTrademark> selectPage(Integer page, Integer limit);

    IPage<SpuInfo> getSpuInfoList(Integer page, Integer limit, Long category3Id);


    List<BaseSaleAttr> getbaseSaleAttrList();

    List<BaseTrademark> getTrademarkList();

    void removeTtademarkById(Long traId);

    void saveSpuInfo(SpuInfo spuInfo);

    void saveTrademark(BaseTrademark baseTrademark);

    BaseTrademark getTtademarkById(Long traId);

    IPage<SkuInfo> getSkuPage(Integer page, Integer limit);

    List<SpuImage> getSpuImageList(Long spuId);


    List<SpuSaleAttr> getspuSaleAttrList(Long spuId);

    void saveSkuInfo(SkuInfo skuInfo);

    void onSale(Long skuId);

    void cancelSale(Long skuId);

    SkuInfo getSkuInfo(Long skuId);

    BaseCategoryView getCategoryView(Long category3Id);

    BigDecimal getSkuPrice(Long skuId);

    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(Long skuId, Long spuId);

    Map getSkuValueIdsMap(Long spuId);
}
