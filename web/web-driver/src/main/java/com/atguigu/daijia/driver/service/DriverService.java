package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;

public interface DriverService {

    /**
     * 小程序授权登录
     * @param code 微信端返回的code
     * @return token
     */
    String login(String code);

    /**
     *
     * 获取司机登录信息
     * @return 司机登录信息
     */
    Result<DriverLoginVo> getDriverLoginInfo(Long driverId);

    /**
     * 获取司机认证信息
     * @param driverId
     * @return
     */
    DriverAuthInfoVo getDriverAuthInfo(Long driverId);
}
