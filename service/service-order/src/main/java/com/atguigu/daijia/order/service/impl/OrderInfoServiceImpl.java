package com.atguigu.daijia.order.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.entity.order.OrderStatusLog;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.order.mapper.OrderInfoMapper;
import com.atguigu.daijia.order.mapper.OrderStatusLogMapper;
import com.atguigu.daijia.order.service.OrderInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderStatusLogMapper orderStatusLogMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 保存订单信息
     *
     * @param orderInfoForm
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long saveOrderInfo(OrderInfoForm orderInfoForm) {
        System.out.println(orderInfoForm.toString());
        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(orderInfoForm, orderInfo);
        //订单号
        String orderNo = UUID.randomUUID().toString().replaceAll("-", "");
        orderInfo.setOrderNo(orderNo);
        orderInfo.setStatus(OrderStatus.WAITING_ACCEPT.getStatus());
        orderInfoMapper.insert(orderInfo);
        this.log(orderInfo.getId(), OrderStatus.WAITING_ACCEPT.getStatus());
        // TODO 接单标识，标识不存在了说明不在等待接单状态了
        return orderInfo.getId();
    }

    /**
     * 根据订单id获取订单状态
     *
     * @param orderId
     * @return
     */
    @Override
    public Integer getOrderStatus(Long orderId) {
//        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
//        return orderInfo.getStatus();
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
        if (orderInfo == null) {
            return OrderStatus.NULL_ORDER.getStatus();
        }
        return orderInfo.getStatus();
    }

    /**
     * 记录订单状态日志
     *
     * @param orderId
     * @param status
     */
    public void log(Long orderId, Integer status) {
        OrderStatusLog orderStatusLog = new OrderStatusLog();
        orderStatusLog.setOrderId(orderId);
        orderStatusLog.setOrderStatus(status);
        orderStatusLog.setOperateTime(new Date());
        orderStatusLogMapper.insert(orderStatusLog);
    }

    // 司机抢单
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean robNewOrder(Long driverId, Long orderId) {
        //抢单成功或取消订单，都会删除该key，redis判断，减少数据库压力
        if (!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)) {
            //抢单失败失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }
        //修改字段
        //update order_info set status = 2, driver_id = #{driverId}, accept_time = now() where id = #{id}
        //进行条件封装
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(driverId);
        orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());
        orderInfo.setDriverId(driverId);
        orderInfo.setAcceptTime(new Date());
        int rows = orderInfoMapper.updateById(orderInfo);
        if (rows != 1) {
            //抢单失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }
        //记录日志
        this.log(orderId,orderInfo.getStatus());

        //删除redis订单标识
        redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK);
        return true;
    }
}
