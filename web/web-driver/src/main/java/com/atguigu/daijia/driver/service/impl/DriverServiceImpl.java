package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.DriverService;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverServiceImpl implements DriverService {

    @Autowired
    private DriverInfoFeignClient client;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;

    /**
     * 小程序授权登录
     *
     * @param code 微信端返回的code   
     * @return token
     */
    @Override
    public String login(String code) {
        Result<Long> longResult = client.login(code);
        //TODO 判断
        Long driverId = longResult.getData();
        //token字符串
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        //放到redis，设置过期时间
        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX + token,
                driverId.toString(),
                RedisConstant.USER_LOGIN_KEY_TIMEOUT,
                TimeUnit.SECONDS);
        return token;
    }

    //获取司机登录信息
    @Override
    public Result<DriverLoginVo> getDriverLoginInfo(Long driverId) {
        return client.getDriverInfo(driverId);
    }

    //司机认证信息
    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {
        Result<DriverAuthInfoVo> authInfoVoResult = client.getDriverAuthInfo(driverId);
        DriverAuthInfoVo driverAuthInfoVo = authInfoVoResult.getData();
        return driverAuthInfoVo;
    }

    //更新司机认证信息
    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        Result<Boolean> booleanResult = client.UpdateDriverAuthInfo(updateDriverAuthInfoForm);
        Boolean data = booleanResult.getData();
        return data;
    }

    //创建司机人脸模型
    @Override
    public Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
        Result<Boolean> booleanResult = client.creatDriverFaceModel(driverFaceModelForm);
        return booleanResult.getData();
    }

    /**
     * 判断司机当日是否进行过人脸识别
     * @param driverId 司机id
     * @return true 已进行过人脸识别，false 未进行过人脸识别
     */
    @Override
    public Boolean isFaceRecognition(Long driverId) {
        return orderInfoFeignClient.isFaceRecognition(driverId).getData();
    }

    /**
     * 验证司机人脸
     * @param driverFaceModelForm 司机调用人脸识别接口传入的参数
     * @return true 验证成功，false 验证失败
     */
    @Override
    public Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm) {
        return client.verifyDriverFace(driverFaceModelForm).getData();
    }
}
