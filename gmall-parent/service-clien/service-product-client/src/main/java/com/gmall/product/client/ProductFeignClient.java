package com.gmall.product.client;

import com.gmall.model.product.BaseCategoryView;
import com.gmall.model.product.SkuInfo;
import com.gmall.model.product.SpuSaleAttr;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient(name = "service-productyc")
public interface ProductFeignClient {

    /**
     * 根据skuId获取sku信息（及图片）
     *
     * @return
     */
    @ApiOperation(" 根据skuId获取sku信息（及图片）")
    @GetMapping("/api/product/inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId);

    /**
     * 通过三级分类id查询分类信息
     * @param category3Id
     * @return
     */
    @ApiOperation("通过三级分类id查询分类信息")
    @GetMapping("/api/product/inner/getBaseCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id);

    /**
     * 获取sku最新价格
     * 对于金钱必须计算精确，因此需要使用BidDecimal
     * @param skuId
     * @return
     */
    @ApiOperation("获取sku最新价格")
    @GetMapping("/api/product/inner/getPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId);

    /**
     * 根据spuId，skuId 查询销售属性集合
     *
     * @return
     */
    @ApiOperation("根据spuId，skuId 查询销售属性集合")
    @GetMapping("/api/product/inner/selectSpuSaleAttrListCheckBySku/{spuId}/{skuId}")
    public List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(@PathVariable Long spuId
            , @PathVariable Long skuId);


    /**
     * 根据spuId 查询map 集合属性
     * @param spuId
     * @return
     */
    @ApiOperation("根据spuId 查询map 集合属性")
    @GetMapping("/api/product/inner/getSaleAttrValuesBySpu/{spuId}")
    public Map getSaleAttrValuesBySpu(@PathVariable Long spuId);

}
