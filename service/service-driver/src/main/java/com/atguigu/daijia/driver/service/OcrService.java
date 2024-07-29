package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.vo.driver.DriverLicenseOcrVo;
import com.atguigu.daijia.model.vo.driver.IdCardOcrVo;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {

    /**
     * 用户上传身份证识别
     * @param file 用户上传文件
     * @return 微信小程序识别上传信息
     */
    IdCardOcrVo idCardOcr(MultipartFile file);

    /**
     * 上传驾驶证识别
     * @param file
     * @return
     */
    DriverLicenseOcrVo driverLicenseOcr(MultipartFile file);
}
