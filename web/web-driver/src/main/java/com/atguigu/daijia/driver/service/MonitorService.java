package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.form.order.OrderMonitorForm;
import org.springframework.web.multipart.MultipartFile;

public interface MonitorService {

    /**
     * 上传
     * @param file 文件
     * @param orderMonitorForm 订单监控bean
     * @return
     */
    Boolean upload(MultipartFile file, OrderMonitorForm orderMonitorForm);
}
