package com.atguigu.daijia.order.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.entity.order.OrderStatusLog;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.order.mapper.OrderInfoMapper;
import com.atguigu.daijia.order.mapper.OrderStatusLogMapper;
import com.atguigu.daijia.order.service.OrderInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderStatusLogMapper orderStatusLogMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 保存订单信息
     *
     * @param orderInfoForm 订单信息
     * @return 订单id
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

        //订单状态
        orderInfo.setStatus(OrderStatus.WAITING_ACCEPT.getStatus());
        orderInfoMapper.insert(orderInfo);

        //记录日志
        this.log(orderInfo.getId(), OrderStatus.WAITING_ACCEPT.getStatus());


        //向Redis中添加接单标识，标识不存在了说明不在等待接单状态了
        redisTemplate
                .opsForValue()
                .set(RedisConstant.ORDER_ACCEPT_MARK,
                        "0",
                        RedisConstant.ORDER_ACCEPT_MARK_EXPIRES_TIME,
                        TimeUnit.MINUTES);

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
    //Redisson分布式锁
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean robNewOrder(Long driverId, Long orderId) {
        System.out.println("-----------------------------------------------------------");
        //抢单成功或取消订单，都会删除该key，redis判断，减少数据库压力
        if (!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)) {
            //抢单失败失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }
        //获取锁
        RLock lock = redissonClient.getLock(RedisConstant.ROB_NEW_ORDER_LOCK + orderId);

        try {
            //获取锁
            boolean flag = lock.tryLock(RedisConstant.ROB_NEW_ORDER_LOCK_WAIT_TIME,
                    RedisConstant.ROB_NEW_ORDER_LOCK_LEASE_TIME,
                    TimeUnit.SECONDS);

            if (flag) {
                if (!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)) {
                    //抢单失败失败
                    throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
                }
                //修改字段
                //update order_info set status = 2, driver_id = #{driverId}, accept_time = now() where id = #{id}
                //进行条件封装
                LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(OrderInfo::getId, orderId);
                //指定版本号
                wrapper.eq(OrderInfo::getStatus, OrderStatus.ACCEPTED.getStatus());
                OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
                //进行数值填充
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
                this.log(orderId, orderInfo.getStatus());
                redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK);
            }
        } catch (Exception e) {
            //抢单失败失败
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        } finally {
            if (lock.isLocked()) {
                lock.unlock();
            }
        }
        //删除redis订单标识
        return true;
    }

    // 乘客端查找当前订单
    @Override
    public CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getCustomerId, customerId);

        //各种状态
        Integer[] statusArray = {
                OrderStatus.ACCEPTED.getStatus(),
                OrderStatus.DRIVER_ARRIVED.getStatus(),
                OrderStatus.UPDATE_CART_INFO.getStatus(),
                OrderStatus.START_SERVICE.getStatus(),
                OrderStatus.END_SERVICE.getStatus(),
                OrderStatus.UNPAID.getStatus()
        };
        wrapper.in(OrderInfo::getStatus, statusArray);
        //获取最新一条数据
        wrapper.orderByDesc(OrderInfo::getId);
        wrapper.last("limit 1");
        //调用接口
        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
        //封装到CurrentOrderInfoVo
        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        if (orderInfo != null) {
            currentOrderInfoVo.setOrderId(orderInfo.getId());
            currentOrderInfoVo.setStatus(orderInfo.getStatus());
            currentOrderInfoVo.setIsHasCurrentOrder(true);
        } else {
            currentOrderInfoVo.setIsHasCurrentOrder(false);
        }
        return currentOrderInfoVo;
    }

    @Override
    public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
        //封装条件
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getDriverId, driverId);
        Integer[] statusArray = {
                OrderStatus.ACCEPTED.getStatus(),
                OrderStatus.DRIVER_ARRIVED.getStatus(),
                OrderStatus.UPDATE_CART_INFO.getStatus(),
                OrderStatus.START_SERVICE.getStatus(),
                OrderStatus.END_SERVICE.getStatus()
        };
        wrapper.in(OrderInfo::getStatus, statusArray);
        wrapper.orderByDesc(OrderInfo::getId);
        wrapper.last(" limit 1");
        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
        //封装到vo
        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        if (null != orderInfo) {
            currentOrderInfoVo.setStatus(orderInfo.getStatus());
            currentOrderInfoVo.setOrderId(orderInfo.getId());
            currentOrderInfoVo.setIsHasCurrentOrder(true);
        } else {
            currentOrderInfoVo.setIsHasCurrentOrder(false);
        }
        return currentOrderInfoVo;
    }

//    // 司机抢单
//    @Transactional(rollbackFor = Exception.class)
//    @Override
//    public Boolean robNewOrderOptimistic(Long driverId, Long orderId) {
//        //抢单成功或取消订单，都会删除该key，redis判断，减少数据库压力
//        if (!redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK)) {
//            //抢单失败失败
//            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
//        }
//        //修改字段
//        //update order_info set status = 2, driver_id = #{driverId}, accept_time = now() where id = #{id}
//        //进行条件封装
//        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(OrderInfo::getId, orderId);
//        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
//        //进行数值填充
//        orderInfo.setId(driverId);
//        orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());
//        orderInfo.setDriverId(driverId);
//        orderInfo.setAcceptTime(new Date());
//        int rows = orderInfoMapper.updateById(orderInfo);
//        if (rows != 1) {
//            //抢单失败
//            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
//        }
//        //记录日志
//        this.log(orderId, orderInfo.getStatus());
//
//        //删除redis订单标识
//        redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK);
//        return true;
//    }


    @Override
    public Boolean driverArriveStartLocation(Long orderId, Long driverId) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.eq(OrderInfo::getDriverId, driverId);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.DRIVER_ARRIVED.getStatus());
        orderInfo.setArriveTime(new Date());
        int rows = orderInfoMapper.update(orderInfo, wrapper);
        if (rows != 1) {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }

    @Override
    public Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, updateOrderCartForm.getOrderId());
        wrapper.eq(OrderInfo::getDriverId, updateOrderCartForm.getDriverId());

        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(updateOrderCartForm, orderInfo);
        orderInfo.setStatus(OrderStatus.UPDATE_CART_INFO.getStatus());

        int rows = orderInfoMapper.update(orderInfo, wrapper);
        if (rows != 1) {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }

    /**
     * 开始代驾
     *
     * @param startDriveForm 订单id和司机id
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean startDrive(StartDriveForm startDriveForm) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, startDriveForm.getOrderId());
        wrapper.eq(OrderInfo::getDriverId, startDriveForm.getDriverId());
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.START_SERVICE.getStatus());
        orderInfo.setStartServiceTime(new Date());

        //只能更新自己的订单
        int rows = orderInfoMapper.update(orderInfo, wrapper);
        if (rows != 1) {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        } else {
            //记录日志
            this.log(startDriveForm.getOrderId(), OrderStatus.START_SERVICE.getStatus());
        }
        return true;
    }
}
