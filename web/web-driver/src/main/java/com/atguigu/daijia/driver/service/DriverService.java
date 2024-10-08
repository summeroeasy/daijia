package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
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

    //更新司机验证信息
    Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm);

    /**
     * 创建司机人脸模型
     * @param driverFaceModelForm
     * @return
     */
    Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm);

    /**
     * 判断司机当日是否进行过人脸识别
     * @param driverId 司机id
     * @return true 已进行过人脸识别，false 未进行过人脸识别
     */
    Boolean isFaceRecognition(Long driverId);

    /**
     * 验证司机人脸
     * @param driverFaceModelForm 司机调用人脸识别接口传入的参数
     * @return true 验证成功，false 验证失败
     */
    Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm);

    /**
     * 开启代驾服务
     * @param driverId 司机id
     * @return
     */
    Boolean startService(Long driverId);

    //停止代驾服务
    Boolean stopService(Long driverId);
}
