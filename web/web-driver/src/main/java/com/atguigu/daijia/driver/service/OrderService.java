package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;

import java.util.List;

public interface OrderService {


    /**
     * 查询订单状态
     * @param orderId
     * @return
     */
    Integer getOrderStatus(Long orderId);

    /**
     * 查询司机新订单数据
     * @param driverId 司机id
     * @return 新订单数据
     */
    List<NewOrderDataVo> findNewOrderQueueData(Long driverId);

    //司机抢单
    Boolean robNewOrder(Long driverId, Long orderId);

    //查找司机端当前订单
    CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId);

    //司机端获取订单信息
    OrderInfoVo getOrderInfo(Long orderId, Long driverId);
}
