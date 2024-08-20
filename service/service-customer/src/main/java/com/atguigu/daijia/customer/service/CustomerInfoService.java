package com.atguigu.daijia.customer.service;

import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface CustomerInfoService extends IService<CustomerInfo> {

    //微信小程序登录接口
    Long login(String code);

    /**
     *
     * 获取登录用户信息接口
     * @param customerId 用户id
     * @return
     */
    CustomerLoginVo getCustomerInfo(Long customerId);

    /**
     * 更新客户微信手机号码
     * @param updateWxPhoneForm 用于存储customerId和wxPhone
     * @return
     */
    Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm);

    /**
     * 获取客户openId
     * @param customerId
     * @return
     */
    String getCustomerOpenId(Long customerId);
}
