package com.atguigu.daijia.order.service;

import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderBillForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderBillVo;
import com.atguigu.daijia.model.vo.order.OrderPayVo;
import com.atguigu.daijia.model.vo.order.OrderProfitsharingVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OrderInfoService extends IService<OrderInfo> {

    /**
     * 保存订单信息
     * @param orderInfoForm
     * @return
     */
    Long saveOrderInfo(OrderInfoForm orderInfoForm);

    /**
     * 根据订单id获取订单状态
     * @param orderId
     * @return
     */
    Integer getOrderStatus(Long orderId);

    // 司机抢单
    Boolean robNewOrder(Long driverId, Long orderId);

    // 乘客端查找当前订单
    CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId);

    //司机端查找当前订单
    CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId);

    //司机到达代驾起始点
    Boolean driverArriveStartLocation(Long orderId, Long driverId);

    //更新订单信息
    Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm);

    /**
     * 开始代驾服务
     * @param startDriveForm 订单id和司机id
     * @return
     */
    Boolean startDrive(StartDriveForm startDriveForm);

    /**
     * 根据时间段获取订单数
     * @param startTime
     * @param endTime
     * @return
     */
    Long getOrderNumByTime(String startTime, String endTime);

    /**
     * 结束代驾服务
     * @param updateOrderBillForm 订单id和司机id
     * @return
     */
    Boolean endDrive(UpdateOrderBillForm updateOrderBillForm);

    /**
     * 获取乘客订单分页列表
     * @param customerId
     * @param pageParam
     * @return
     */
    PageVo findCustomerOrderPage(Page<OrderInfo> pageParam, Long customerId);

    /**
     * 获取司机订单分页列表
     * @param driverId
     * @param pageParam
     * @return
     */
    PageVo findDriverOrderPage(Page<OrderInfo> pageParam, Long driverId);

    /**
     * 获取订单账单信息
     * @param orderId
     * @return
     */
    OrderBillVo getOrderBillInfo(Long orderId);

    /**
     * 获取订单分账信息
     * @param orderId
     * @return
     */
    OrderProfitsharingVo getOrderProfitsharing(Long orderId);

    /**
     * 发送订单账单信息
     * @param orderId
     * @param driverId
     * @return
     */
    Boolean sendOrderBillInfo(Long orderId, Long driverId);

    /**
     * 获取订单支付信息
     * @param orderNo
     * @param customerId
     * @return
     */
    OrderPayVo getOrderPayVo(String orderNo, Long customerId);

    //基于乐观锁的司机抢单
    //Boolean robNewOrderOptimistic(Long driverId, Long orderId);
}
