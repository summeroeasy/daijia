package com.atguigu.daijia.payment.service;

import com.atguigu.daijia.model.form.payment.PaymentInfoForm;
import com.atguigu.daijia.model.vo.payment.WxPrepayVo;
import jakarta.servlet.http.HttpServletRequest;

public interface WxPayService {


    /**
     * 创建微信支付订单
     * @param paymentInfoForm
     * @return
     */
    WxPrepayVo createWxPayment(PaymentInfoForm paymentInfoForm);


    Object queryPayStatus(String orderNo);


    void wxnotify(HttpServletRequest request);

    void handleOrder(String orderNo);
}
