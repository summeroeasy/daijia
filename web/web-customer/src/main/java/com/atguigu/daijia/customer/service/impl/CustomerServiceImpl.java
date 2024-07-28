package com.atguigu.daijia.customer.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.login.GuiguLogin;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.customer.service.CustomerService;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerInfoFeignClient client;

    @Autowired
    private RedisTemplate redisTemplate;

    // 调用远程接口
    @Override
    public String login(String code) {
        //1 拿着code进行远程调用, 返回用户id
        Result<Long> longResult = client.login(code);
        //2 判断如果返回失败了,返回错误提示
        Integer codeResult = longResult.getCode();
        if (!codeResult.equals(ResultCodeEnum.SUCCESS.getCode())) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        //3 获取远程调用返回用户id
        Long customerId = longResult.getData();
        //4 判断返回用户id是否为空,如果为空,返回错误提示
        if (customerId == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        //5 生成token字符串
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        //6 把用户id放到Redis中,并设置过期时间
        //redisTemplate.opsForValue().set(token,customerId.toString(),30, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX + token,
                customerId.toString(),
                RedisConstant.USER_LOGIN_KEY_TIMEOUT,
                TimeUnit.SECONDS);
        //7 返回token字符串
        return token;
    }

    /**
     * 获取用户登陆信息
     * @param token 根据用户请求一起传递,在请求头中
     * @return
     */
    @Override
    public CustomerLoginVo getCustomerLoginInfo(String token) {
        //根据token查redis
        //查询token在redis里面对应用户id
        String customerId = (String) redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);
        if (StringUtils.isEmpty(customerId)) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        //根据用户id进行远程调用 得到用户信息
        Result<CustomerLoginVo> customerLoginInfoResult = client.getCustomerLoginInfo(Long.parseLong(customerId));
        Integer code = customerLoginInfoResult.getCode();
        if (!code.equals(ResultCodeEnum.SUCCESS.getCode())) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        CustomerLoginVo data = customerLoginInfoResult.getData();
        if (data == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        return data;
    }

    @Override
    public CustomerLoginVo getCustomerInfo(Long customerId) {
        //根据用户id进行远程调用 得到用户信息
        Result<CustomerLoginVo> customerLoginInfoResult = client.getCustomerLoginInfo(customerId);
        Integer code = customerLoginInfoResult.getCode();
        if (!code.equals(ResultCodeEnum.SUCCESS.getCode())) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        CustomerLoginVo data = customerLoginInfoResult.getData();
        if (data == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        return data;
    }

    /**
     * 更新用户微信手机号
     * @param updateWxPhoneForm 用于存储customerId和wxPhone
     * @return
     */
    @Override
    public Object updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm) {
        client.updateWxPhoneNumber(updateWxPhoneForm);
        return true;
    }
}
