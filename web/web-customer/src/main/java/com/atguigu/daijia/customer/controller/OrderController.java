package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.login.GuiguLogin;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.customer.service.OrderService;
import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.payment.CreateWxPaymentForm;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import com.atguigu.daijia.model.vo.payment.WxPrepayVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "订单API接口管理")
@RestController
@RequestMapping("/order")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Operation(summary = "查找乘客端当前订单")
    @GuiguLogin
    @GetMapping("/searchCustomerCurrentOrder")
    public Result<CurrentOrderInfoVo> searchCustomerCurrentOrder() {
        Long customerId = AuthContextHolder.getUserId();
        return Result.ok(orderService.searchCustomerCurrentOrder(customerId));
    }

    @Operation(summary = "预估订单数据")
    @GuiguLogin
    @PostMapping("/expectOrder")
    public Result<ExpectOrderVo> expectOrder(@RequestBody ExpectOrderForm expectOrderForm) {
        return Result.ok(orderService.expectOrder(expectOrderForm));
    }

    @Operation(summary = "乘客下单")
    @GuiguLogin
    @PostMapping("/submitOrder")
    public Result<Long> submitOrder(@RequestBody SubmitOrderForm submitOrderForm) {
        log.info("乘客下单：{}", submitOrderForm.toString());
        submitOrderForm.setCustomerId(AuthContextHolder.getUserId());
        return Result.ok(orderService.submitOrder(submitOrderForm));
    }

    @Operation(summary = "查询订单状态")
    @GuiguLogin
    @GetMapping("/getOrderStatus/{orderId}")
    public Result<Integer> getOrderStatus(@PathVariable Long orderId) {
        return Result.ok(orderService.getOrderStatus(orderId));
    }

    @Operation(summary = "获取订单信息")
    @GuiguLogin
    @GetMapping("/getOrderInfo/{orderId}")
    public Result<OrderInfoVo> getOrderInfo(@PathVariable Long orderId) {
        Long customerId = AuthContextHolder.getUserId();
        return Result.ok(orderService.getOrderInfo(orderId, customerId));
    }

    @Operation(summary = "根据订单id获取司机基本信息")
    @GuiguLogin
    @GetMapping("/getDriverInfo/{orderId}")
    public Result<DriverInfoVo> getDriverInfo(@PathVariable Long orderId) {
        Long customerId = AuthContextHolder.getUserId();
        return Result.ok(orderService.getDriverInfo(orderId, customerId));
    }

    @Operation(summary = "司机赶往代驾起始点：获取订单经纬度位置")
    @GetMapping("/getCacheOrderLocation/{orderId}")
    public Result<OrderLocationVo> getCacheOrderLocation(@PathVariable Long orderId) {
        return Result.ok(orderService.getCacheOrderLocation(orderId));
    }

    @Operation(summary = "计算最佳驾驶线路")
    @GuiguLogin
    @PostMapping("/calculateDrivingLine")
    public Result<DrivingLineVo> calculateDrivingLine(@RequestBody CalculateDrivingLineForm calculateDrivingLineForm) {
        return Result.ok(orderService.calculateDrivingLine(calculateDrivingLineForm));
    }

    @Operation(summary = "代驾服务：获取订单服务最后一个位置信息")
    @GuiguLogin
    @GetMapping("/getOrderServiceLastLocation/{orderId}")
    public Result<OrderServiceLastLocationVo> getOrderServiceLastLocation(@PathVariable Long orderId) {
        return Result.ok(orderService.getOrderServiceLastLocation(orderId));
    }

    /**
     * 创建微信支付
     *
     * 本接口用于初始化微信支付订单在微信支付平台的创建本接口通过Post请求接收创建微信支付订单所需的信息，
     * 并返回微信支付的预支付交易会话标识（Prepay_id），该标识用于后续的微信支付操作
     *
     * 注意：本接口需要用户处于登录状态，以便从上下文中获取当前用户的ID作为支付订单的客户ID
     *
     * @param createWxPaymentForm 包含创建微信支付订单所需信息的表单对象
     * @return 包含微信支付预支付交易会话标识的Result对象如果操作成功，Result对象的状态码将为200，
     *         并包含一个WxPrepayVo对象，其中包含预支付_id_如果操作失败，Result对象将包含相应的错误码和信息
     */
    @Operation(summary = "创建微信支付")
    @GuiguLogin
    @PostMapping("/createWxPayment")
    public Result<WxPrepayVo> createWxPayment(@RequestBody CreateWxPaymentForm createWxPaymentForm) {
        // 从安全上下文中获取当前用户的ID作为客户ID
        Long customerId = AuthContextHolder.getUserId();
        // 将获取到的客户ID设置到表单对象中，以便在创建支付订单时使用
        createWxPaymentForm.setCustomerId(customerId);
        // 调用orderService的createWxPayment方法创建微信支付订单，并将返回结果封装在Result对象中返回
        return Result.ok(orderService.createWxPayment(createWxPaymentForm));
    }

    /**
     * 查询支付状态
     * 通过订单号查询支付状态，需要用户登录状态
     *
     * @param orderNo 订单号，用于查询支付状态
     * @return 包含支付状态的Result对象，支付成功返回true，否则返回false
     */
    @Operation(summary = "支付状态查询")
    @GuiguLogin
    @GetMapping("/queryPayStatus/{orderNo}")
    public Result<Boolean> queryPayStatus(@PathVariable String orderNo) {
        return Result.ok(orderService.queryPayStatus(orderNo));
    }
}

