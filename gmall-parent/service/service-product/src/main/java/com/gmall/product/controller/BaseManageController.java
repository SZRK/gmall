package com.gmall.product.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gmall.common.result.Result;
import com.gmall.model.product.*;
import com.gmall.product.service.ManageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Api("商品基础属性接口")
@RestController
//@CrossOrigin
@RequestMapping("/admin/product")
public class BaseManageController {

    @Autowired
    private ManageService manageService;

    @ApiOperation("获取一级分类")
    @GetMapping("/getCategory1")
    public Result<List<BaseCategory1>> getCategory1() {
        List<BaseCategory1> list = manageService.getCategory1();
        return Result.ok(list);
    }

    @ApiOperation("根据一级分类Id获取二级分类")
    @GetMapping("/getCategory2/{category1Id}")
    public Result<List<BaseCategory2>> getCategory2(@PathVariable Long category1Id) {
        List<BaseCategory2> list = manageService.getCategory2(category1Id);
        return Result.ok(list);
    }

    @ApiOperation("根据二级分类Id获取三级分类")
    @GetMapping("/getCategory3/{category2Id}")
    public Result<List<BaseCategory3>> getCategory3(@PathVariable Long category2Id) {
        List<BaseCategory3> list = manageService.getCategory3(category2Id);
        return Result.ok(list);
    }

    @ApiOperation("根据分类id获取平台属性")
    @GetMapping("/attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result<List<BaseAttrInfo>> attrInfoList(@PathVariable("category1Id") Long category1Id
            , @PathVariable("category2Id") Long category2Id
            , @PathVariable("category3Id") Long category3Id) {
        List<BaseAttrInfo> list = manageService.attrInfoList(category1Id, category2Id, category3Id);
        return Result.ok(list);
    }


    @ApiOperation("添加平台属性")
    @PostMapping("/saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo) {
        manageService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }


    @ApiOperation("根据平台属性ID获取平台属性")
    @GetMapping("/getAttrValueList/{arrtId}")
    public Result getAttrValueList(@PathVariable Long arrtId) {
        List<BaseAttrValue> list = manageService.getAttrValueList(arrtId);
        return Result.ok(list);
    }


    @ApiOperation("获取品牌分页数据")
    @GetMapping("/baseTrademark/{page}/{limit}")
    public Result selectPage(@PathVariable Integer page, @PathVariable Integer limit) {
        IPage<BaseTrademark> iPage = manageService.selectPage(page, limit);
        return Result.ok(iPage);
    }


    @ApiOperation("根据平台ID删除品牌")
    @DeleteMapping("/baseTrademark/remove/{traId}")
    public Result removeTrademarkById(@PathVariable Long traId) {
        manageService.removeTtademarkById(traId);
        return Result.ok();
    }

    @ApiOperation("新增品牌")
    @PostMapping("/baseTrademark/save")
    public Result saveTrademark(@RequestBody BaseTrademark baseTrademark) {
        manageService.saveTrademark(baseTrademark);
        return Result.ok();
    }

    @ApiOperation("根据平台ID获取品牌")
    @GetMapping("/baseTrademark/get/{traId}")
    public Result getTrademarkById(@PathVariable Long traId) {
        BaseTrademark baseTtademark = manageService.getTtademarkById(traId);
        return Result.ok(baseTtademark);
    }

    @ApiOperation("获取spu分页列表")
    @GetMapping("/{page}/{limit}")
    public Result getSpuInfoList(@PathVariable Integer page, @PathVariable Integer limit, Long category3Id) {
        IPage<SpuInfo> infoIPage = manageService.getSpuInfoList(page, limit, category3Id);
        return Result.ok(infoIPage);
    }

    @ApiOperation("获取平台销售属性")
    @GetMapping("/baseSaleAttrList")
    public Result getbaseSaleAttrList() {
        List<BaseSaleAttr> list = manageService.getbaseSaleAttrList();
        return Result.ok(list);
    }


    @ApiOperation("获取品牌")
    @GetMapping("/baseTrademark/getTrademarkList")
    public Result getTrademarkList() {
        List<BaseTrademark> list = manageService.getTrademarkList();
        return Result.ok(list);
    }

    @ApiOperation("保存Spu")
    @PostMapping("/saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo) {
        manageService.saveSpuInfo(spuInfo);
        return Result.ok();
    }


    @ApiOperation("获取sku分页列表")
    @GetMapping("/list/{page}/{limit}")
    public Result getSkuPage(@PathVariable Integer page, @PathVariable Integer limit) {
        IPage<SkuInfo> skuList = manageService.getSkuPage(page, limit);
        return Result.ok(skuList);
    }


    @ApiOperation("根据spuId获取图片列表")
    @GetMapping("/spuImageList/{spuId}")
    public Result getSpuImageList(@PathVariable Long spuId) {
        List<SpuImage> imageList = manageService.getSpuImageList(spuId);
        return Result.ok(imageList);
    }

    @ApiOperation("根据spuId获取销售属性")
    @GetMapping("/spuSaleAttrList/{spuId}")
    public Result getspuSaleAttrList(@PathVariable Long spuId) {
        List<SpuSaleAttr> skuInfoList = manageService.getspuSaleAttrList(spuId);
        return Result.ok(skuInfoList);
    }

    @ApiOperation("添加sku")
    @PostMapping("/saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo) {
        manageService.saveSkuInfo(skuInfo);
        return Result.ok();
    }


    @ApiOperation("上架")
    @GetMapping("/onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId) {
        manageService.onSale(skuId);
        return Result.ok();
    }

    @ApiOperation("下架")
    @GetMapping("/cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId) {
        manageService.cancelSale(skuId);
        return Result.ok();
    }

}


