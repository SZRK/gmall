package com.gmall.product.controller;

import com.gmall.common.result.Result;
import com.gmall.product.utils.FastDFSUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Api("文件上传下载及删除")
@RestController
@RequestMapping("/admin/product/")
public class FileController {
    @ApiOperation("上传图片")
    @PostMapping("/fileUpload")
    public Result fileUpload(MultipartFile file) {
        String imgUrl = FastDFSUtils.fileUpload(file);
        return Result.ok(imgUrl);
    }
}
