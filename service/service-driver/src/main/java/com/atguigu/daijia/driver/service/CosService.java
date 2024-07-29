package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import org.springframework.web.multipart.MultipartFile;

public interface CosService {


    /**
     * 上传
     * @param file 上传文件
     * @param path 上传路径
     * @return
     */
    CosUploadVo upload(MultipartFile file, String path);

    // 获取图片url
    String getImageUrl(String path);
}
