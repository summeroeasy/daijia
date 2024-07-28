package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.DriverService;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
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

    @Override
    public Result<DriverLoginVo> getDriverLoginInfo(Long driverId) {
        return client.getDriverInfo(driverId);
    }


}
