package com.gmall.product.controller;

import com.gmall.common.result.Result;
import com.gmall.model.product.BaseAttrInfo;
import com.gmall.model.product.BaseCategory1;
import com.gmall.model.product.BaseCategory2;
import com.gmall.model.product.BaseCategory3;
import com.gmall.product.service.ManageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.PipedReader;
import java.util.List;

@Api("商品基础属性接口")
@RestController
//@CrossOrigin
@RequestMapping("/admin/product")
public class BaseManageController {

    @Autowired
    private ManageService ManageService ;

    @ApiOperation("获取一级分类")
    @GetMapping("/getCategory1")
    public Result<List<BaseCategory1>> getCategory1() {
        List<BaseCategory1> list = ManageService .getCategory1();
        return Result.ok(list);
    }
    @ApiOperation("根据一级分类Id获取二级分类")
    @GetMapping("/getCategory2/{category1Id}")
    public Result<List<BaseCategory2>> getCategory2(@PathVariable Long category1Id ) {
        List<BaseCategory2> list = ManageService.getCategory2(category1Id);
        return Result.ok(list);
    }

    @ApiOperation("根据二级分类Id获取三级分类")
    @GetMapping("/getCategory3/{category2Id}")
    public Result<List<BaseCategory3>> getCategory3(@PathVariable Long category2Id) {
        List<BaseCategory3> list = ManageService.getCategory3(category2Id);
        return Result.ok(list);
    }
    @ApiOperation("根据分类id获取平台属性")
    @GetMapping("/attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result<List<BaseAttrInfo>> attrInfoList(@PathVariable("category1Id") Long category1Id
                                    ,@PathVariable("category2Id") Long category2Id
                                     ,@PathVariable("category3Id") Long category3Id) {
        List<BaseAttrInfo> list = ManageService.attrInfoList(category1Id, category2Id, category3Id);
        return Result.ok(list);
    }

}


