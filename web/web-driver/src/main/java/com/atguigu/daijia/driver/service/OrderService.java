package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderFeeForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
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

    //计算司机最佳行驶路线
    DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm);

    //司机到达代驾起始点
    Boolean driverArriveStartLocation(Long orderId, Long driverId);

    //更新代驾车辆信息
    Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm);

    //开启代驾服务
    Boolean startDrive(StartDriveForm startDriveForm);

    //结束代驾服务更新订单账单
    Boolean endDrive(OrderFeeForm orderFeeForm);

    //获取司机端订单账单详细信息
    PageVo findDriverOrderPage(Long driverId, Long page, Long limit);

    //多线程获取乘客端订单账单详细信息
    public Boolean endDriveThread(OrderFeeForm orderFeeForm);

    Boolean sendOrderBillInfo(Long orderId, Long driverId);
}
