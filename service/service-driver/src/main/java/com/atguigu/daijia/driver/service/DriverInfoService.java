package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.entity.driver.DriverInfo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface DriverInfoService extends IService<DriverInfo> {

    /**
     * 小程序授权登录
     * @param code 小程序授权码
     * @return 司机id
     */
    Long login(String code);

    /**
     * 获取司机登录信息
     * @param driverId 司机id
     * @return 司机登录信息
     */
    DriverLoginVo getDriverInfo(Long driverId);
}
