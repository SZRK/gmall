package com.gmall.list.controller;

import com.gmall.common.result.Result;
import com.gmall.list.service.ListYcService;
import com.gmall.model.list.SearchParam;
import com.gmall.model.list.SearchResponseVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/list")
public class LsitApiController {

    @Autowired
    private ListYcService listYcService;

    /**
     *创建ES索引库
     * @return
     */
    @ApiOperation("创建Es索引库")
    @GetMapping("/createIndex")
    public Result createIndex() {
        listYcService.createIndex();
        return Result.ok();
    }

    // 商品上架
    @ApiOperation("上架商品")
    @GetMapping("/onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId) {
        listYcService.onSale(skuId);
        return  Result.ok();
    }

    // 商品下架
    @ApiOperation("下架商品")
    @GetMapping("/cancelSale/{skuId}")
    public Result cancelSale(@PathVariable  Long skuId) {
        listYcService.cancelSale(skuId);
        return Result.ok();
    }

    // 修改评分,利用redis缓存评分信息，待到达一定的评分是更新到es中
    @ApiOperation("更新商品评分")
    @GetMapping("inner/incrHotScore/{skuId}")
    public Result incrHotScore(@PathVariable("skuId") Long skuId) {
        listYcService.incrHotScore(skuId);
        return Result.ok();
    }

    @ApiOperation("搜索商品")
    @PostMapping("/search")
    public SearchResponseVo search(@RequestBody SearchParam searchParam) {
         return listYcService.search(searchParam);
    }



}
