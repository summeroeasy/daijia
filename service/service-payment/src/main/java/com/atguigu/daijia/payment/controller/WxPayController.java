package com.atguigu.daijia.payment.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.form.payment.PaymentInfoForm;
import com.atguigu.daijia.model.vo.payment.WxPrepayVo;
import com.atguigu.daijia.payment.service.WxPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "微信支付接口")
@RestController
@RequestMapping("payment/wxPay")
@Slf4j
public class WxPayController {

    @Autowired
    private WxPayService wxPayService;

    // 创建微信支付
    // 该接口用于通过POST请求创建微信支付
    // 使用@Operation注解提供接口的摘要信息
    // 使用@PostMapping注解指定处理创建微信支付的HTTP请求方法和路径
    // @RequestBody注解用于接收和解析请求体中的支付信息表单
    // 返回一个封装了预支付信息的Result对象
    @Operation(summary = "创建微信支付")
    @PostMapping("/createJsapi")
    public Result<WxPrepayVo> createWxPayment(@RequestBody PaymentInfoForm paymentInfoForm) {
        return Result.ok(wxPayService.createWxPayment(paymentInfoForm));
    }
}