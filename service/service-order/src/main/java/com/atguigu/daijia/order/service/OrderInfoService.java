package com.atguigu.daijia.order.service;

import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
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

    //基于乐观锁的司机抢单
    Boolean robNewOrderOptimistic(Long driverId, Long orderId);
}
