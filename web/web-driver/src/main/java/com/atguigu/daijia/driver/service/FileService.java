package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    //文件上传接口
    CosUploadVo uploadFile(MultipartFile file, String path);
}
