package com.atguigu.daijia.order.service;

import com.atguigu.daijia.model.entity.order.OrderMonitor;
import com.atguigu.daijia.model.entity.order.OrderMonitorRecord;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OrderMonitorService extends IService<OrderMonitor> {

    /**
     * 保存订单监控记录数据
     * @param orderMonitorRecord 订单监控记录数据参数
     * @return 保存结果
     */
    Boolean saveOrderMonitorRecord(OrderMonitorRecord orderMonitorRecord);

    /**
     * 保存订单监控数据
     * @param orderMonitor 订单监控数据
     * @return 保存结果
     */
    Long saveOrderMonitor(OrderMonitor orderMonitor);

    /**
     * 获取订单监控数据
     * @param orderId 订单ID
     * @return 订单监控数据
     */
    OrderMonitor getOrderMonitor(Long orderId);

    /**
     * 更新订单监控数据
     * @param orderMonitor 订单监控数据
     * @return 更新结果
     */
    Boolean updateOrderMonitor(OrderMonitor orderMonitor);


}
