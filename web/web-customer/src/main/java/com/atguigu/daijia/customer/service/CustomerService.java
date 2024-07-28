package com.atguigu.daijia.customer.service;

import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;

public interface CustomerService {


    // 小程序登录
    String login(String code);

    /**
     * 获取用户登陆信息
     * @param token 根据用户请求一起传递,在请求头中
     * @return
     */
    CustomerLoginVo getCustomerLoginInfo(String token);

    //获取用户信息
    CustomerLoginVo getCustomerInfo(Long customerId);

    /**
     * 更新用户微信手机号
     * @param updateWxPhoneForm 用于存储customerId和wxPhone
     * @return
     */
    Object updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm);
}
