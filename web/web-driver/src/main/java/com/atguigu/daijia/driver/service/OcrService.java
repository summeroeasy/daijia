package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.vo.driver.DriverLicenseOcrVo;
import com.atguigu.daijia.model.vo.driver.IdCardOcrVo;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {

    /**
     * 远程调用service-driver
     * @param file
     * @return
     */
    IdCardOcrVo idCardOcr(MultipartFile file);

    DriverLicenseOcrVo driverLicenseOcr(MultipartFile file);
}
