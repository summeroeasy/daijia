package com.atguigu.daijia.payment.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.form.payment.PaymentInfoForm;
import com.atguigu.daijia.model.vo.payment.WxPrepayVo;
import com.atguigu.daijia.payment.service.WxPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


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

    @Operation(summary = "支付状态查询")
    @GetMapping("/queryPayStatus/{orderNo}")
    /**
     * 查询支付状态
     * 通过订单号查询支付状态
     *
     * @param orderNo 订单号，用于标识特定的支付订单
     * @return 返回支付状态的查询结果，使用Result包装返回，包含状态码和状态信息
     */
    public Result queryPayStatus(@PathVariable String orderNo) {
        return Result.ok(wxPayService.queryPayStatus(orderNo));
    }

    @Operation(summary = "微信支付异步通知接口")
    @PostMapping("/notify")
    public Map<String,Object> notify(HttpServletRequest request) {
        try {
            wxPayService.wxnotify(request);

            //返回成功
            Map<String,Object> result = new HashMap<>();
            result.put("code", "SUCCESS");
            result.put("message", "成功");
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }

        //返回失败
        Map<String,Object> result = new HashMap<>();
        result.put("code", "FAIL");
        result.put("message", "失败");
        return result;
    }
}