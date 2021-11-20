package com.gmall.product.controller;

import com.gmall.model.product.*;
import com.gmall.product.service.ManageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
public class ProdApiController {

    @Autowired
    private ManageService manageService;

    /**
     * 根据skuId获取sku信息（及图片）
     *
     * @return
     */
    @ApiOperation(" 根据skuId获取sku信息（及图片）")
    @GetMapping("inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId) {
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        return skuInfo;
    }

    /**
     * 通过三级分类id查询分类信息
     * @param category3Id
     * @return
     */
    @ApiOperation("通过三级分类id查询分类信息")
    @GetMapping("inner/getBaseCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id) {
        BaseCategoryView baseCategoryView =  manageService.getCategoryView(category3Id);
        return baseCategoryView;
    }

    /**
     * 获取sku最新价格
     * 对于金钱必须计算精确，因此需要使用BidDecimal
     * @param skuId
     * @return
     */
    @ApiOperation("获取sku最新价格")
    @GetMapping("inner/getPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId) {
       BigDecimal price =  manageService.getSkuPrice(skuId);
        return price;
    }

    /**
     * 根据spuId，skuId 查询销售属性集合
     *
     * @return
     */
    @ApiOperation("根据spuId，skuId 查询销售属性集合")
    @GetMapping("inner/selectSpuSaleAttrListCheckBySku/{spuId}/{skuId}")
    public List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(@PathVariable Long spuId, @PathVariable Long skuId) {
        List<SpuSaleAttr> list = manageService.selectSpuSaleAttrListCheckBySku(skuId, spuId);
        return list;
    }

    /**
     * 根据spuId 查询map 集合属性
     * @param spuId
     * @return
     */
    @ApiOperation("根据spuId 查询map 集合属性")
    @GetMapping("inner/getSaleAttrValuesBySpu/{spuId}")
    public Map getSaleAttrValuesBySpu(@PathVariable Long spuId) {
        Map map= manageService.getSkuValueIdsMap(spuId);
        return map;
    }


}
