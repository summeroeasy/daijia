package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.entity.driver.DriverInfo;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
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
     * 获取代驾登录信息
     * @param driverId 司机id
     * @return 司机登录信息
     */
    DriverLoginVo getDriverInfo(Long driverId);

    /**
     * 获取代驾验证信息
     * @param driverId
     * @return
     */
    DriverAuthInfoVo getDriverAuthInfo(Long driverId);

    //更新代驾验证信息
    Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm);

    /**
     * 创建代驾人脸模型
     * @param driverFaceModelForm
     * @return
     */
    Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm);

    /**
     * 获取代驾个性设置
     * @param driverId
     * @return
     */
    DriverSet getDriverSet(Long driverId);

    /**
     * 判断司机当日是否进行过人脸识别
     * @param driverId 司机id
     * @return true:是 false:否
     */
    Boolean isFaceRecognition(Long driverId);

    /**
     * 验证司机人脸
     * @param driverFaceModelForm 司机人脸模型bean
     * @return true:是 false:否
     */
    Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm);

    /**
     * 更新代驾服务状态
     * @param driverId 司机id
     * @param status 司机状态
     * @return true:成功 false:失败
     */
    Boolean updateServiceStatus(Long driverId, Integer status);
}
