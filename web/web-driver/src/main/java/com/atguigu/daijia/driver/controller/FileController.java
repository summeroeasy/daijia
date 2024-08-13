package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "上传管理接口")
@RestController
@RequestMapping("file")
public class FileController {

    @Autowired
    private FileService fileService;

    //文件上传接口
//    @Operation(summary = "上传")
//    //@GuiguLogin
//    @PostMapping("/upload")
//    public Result<String> upload(@RequestPart("file") MultipartFile file,
//                                      @RequestParam(name = "path",defaultValue = "auth") String path) {
//        CosUploadVo cosUploadVo = cosService.uploadFile(file,path);
//        String showUrl = cosUploadVo.getShowUrl();
//        return Result.ok(showUrl);
//    }

    @Operation(summary = "上传")
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file){
        String url = fileService.uploadFile(file);
        return Result.ok(url);
    }

}
