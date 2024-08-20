package com.atguigu.daijia.order.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.model.entity.order.*;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderBillForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderBillVo;
import com.atguigu.daijia.model.vo.order.OrderListVo;
import com.atguigu.daijia.model.vo.order.OrderProfitsharingVo;
import com.atguigu.daijia.order.mapper.OrderBillMapper;
import com.atguigu.daijia.order.mapper.OrderInfoMapper;
import com.atguigu.daijia.order.mapper.OrderProfitsharingMapper;
import com.atguigu.daijia.order.mapper.OrderStatusLogMapper;
import com.atguigu.daijia.order.service.OrderInfoService;
import com.atguigu.daijia.order.service.OrderMonitorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    @Autowired
    private OrderMonitorService orderMonitorService;

    @Autowired
    private OrderBillMapper orderBillMapper;

    @Autowired
    private OrderProfitsharingMapper orderProfitsharingMapper;

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
        //初始化订单监控统计数据
        OrderMonitor orderMonitor = new OrderMonitor();
        orderMonitor.setOrderId(startDriveForm.getOrderId());
        orderMonitorService.saveOrderMonitor(orderMonitor);
        return true;
    }

    /**
     * 根据时间段获取订单数
     * 本方法通过统计在指定时间段内开始服务的订单数量，来帮助分析业务情况
     * 使用了Lambda表达式来构建查询条件，提高代码的可读性和简洁性
     *
     * @param startTime 开始时间，格式为字符串，用于指定查询区间的起始点
     * @param endTime   结束时间，格式为字符串，用于指定查询区间的结束点
     * @return 返回在指定时间段内的订单总数
     */
    @Override
    public Long getOrderNumByTime(String startTime, String endTime) {
        // 创建查询构造器，用于后续设置查询条件
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        // 设置查询条件，筛选出开始服务时间大于等于startTime的订单
        wrapper.ge(OrderInfo::getStartServiceTime, startTime);
        // 进一步筛选，找出开始服务时间小于等于endTime的订单
        wrapper.le(OrderInfo::getStartServiceTime, endTime);
        // 执行查询，获取符合条件的订单数量
        Long count = orderInfoMapper.selectCount(wrapper);
        // 返回订单数量
        return count;
    }

    /**
     * 结束行程
     *
     * @param updateOrderBillForm 表单数据，包含更新订单和账单的信息
     * @return Boolean 表示订单结束操作是否成功的布尔值
     */
    @Override
    public Boolean endDrive(UpdateOrderBillForm updateOrderBillForm) {
        // 1.更新订单信息
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, updateOrderBillForm.getOrderId());
        wrapper.eq(OrderInfo::getDriverId, updateOrderBillForm.getDriverId());

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.END_SERVICE.getStatus());
        orderInfo.setRealAmount(updateOrderBillForm.getTotalAmount());
        orderInfo.setFavourFee(updateOrderBillForm.getFavourFee());
        orderInfo.setRealDistance(updateOrderBillForm.getRealDistance());
        orderInfo.setEndServiceTime(new Date());

        int rows = orderInfoMapper.update(orderInfo, wrapper);

        if (rows == 1) {
            //添加账单数据
            OrderBill orderBill = new OrderBill();
            BeanUtils.copyProperties(updateOrderBillForm, orderBill);
            orderBill.setOrderId(updateOrderBillForm.getOrderId());
            orderBill.setPayAmount(updateOrderBillForm.getTotalAmount());
            orderBillMapper.insert(orderBill);

            //添加分账信息
            OrderProfitsharing orderProfitsharing = new OrderProfitsharing();
            BeanUtils.copyProperties(updateOrderBillForm, orderProfitsharing);
            orderProfitsharing.setOrderId(updateOrderBillForm.getOrderId());
            orderProfitsharing.setRuleId(updateOrderBillForm.getProfitsharingRuleId());
            orderProfitsharing.setStatus(1);
            orderProfitsharingMapper.insert(orderProfitsharing);

        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return true;
    }


    /**
     * 根据客户ID查询客户订单分页信息
     * 该方法通过接收页码和客户ID，返回一个包含订单信息的分页对象
     *
     * @param pageParam  分页参数，包含了页码、每页数量等信息
     * @param customerId 客户ID，用于筛选特定客户的订单
     * @return 返回一个PageVo对象，其中包含了订单信息的分页数据
     */
    @Override
    public PageVo findCustomerOrderPage(Page<OrderInfo> pageParam, Long customerId) {
        IPage<OrderListVo> pageInfo = orderInfoMapper.selectCustomerOrderPage(pageParam, customerId);
        return new PageVo<>(pageInfo.getRecords(),pageInfo.getPages(),pageInfo.getTotal());
    }

    /**
     * 根据司机ID查询订单信息分页
     * 此方法用于根据提供的司机ID，查询并返回一个分页的订单信息对象
     * 它利用分页参数来控制返回的订单信息的数量和起始位置，以提高数据处理的效率和用户体验
     *
     * @param pageParam 分页参数对象，用于控制查询的分页行为，如当前页码和每页数量
     * @param driverId 司机的ID，用于定位特定司机的订单信息
     * @return 返回一个PageVo对象，包含按分页查询到的订单信息，或者在特定情况下返回null
     */
    @Override
    public PageVo findDriverOrderPage(Page<OrderInfo> pageParam, Long driverId) {
        IPage<OrderListVo> pageInfo =  orderInfoMapper.selectDriverOrderPage(pageParam,driverId);
        return new PageVo<>(pageInfo.getRecords(),pageInfo.getPages(),pageInfo.getTotal());
    }

    /**
     * 获取订单账单信息
     * @param orderId
     * @return
     */
    @Override
    public OrderBillVo getOrderBillInfo(Long orderId) {
        LambdaQueryWrapper<OrderBill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderBill::getOrderId,orderId);
        OrderBill orderBill = orderBillMapper.selectOne(wrapper);
        if (orderBill == null){
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        OrderBillVo orderBillVo = new OrderBillVo();
        BeanUtils.copyProperties(orderBill,orderBillVo);
        return orderBillVo;
    }

    @Override
    public OrderProfitsharingVo getOrderProfitsharing(Long orderId) {
        LambdaQueryWrapper<OrderProfitsharing> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderProfitsharing::getOrderId,orderId);
        OrderProfitsharing orderProfitsharing = orderProfitsharingMapper.selectOne(wrapper);

        OrderProfitsharingVo orderProfitsharingVo = new OrderProfitsharingVo();
        BeanUtils.copyProperties(orderProfitsharing,orderProfitsharingVo);
        return orderProfitsharingVo;
    }

    @Override
    public Boolean sendOrderBillInfo(Long orderId, Long driverId) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId,orderId);
        wrapper.eq(OrderInfo::getDriverId,driverId);

        //更新字段
        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setStatus(OrderStatus.UNPAID.getStatus());
        int row = orderInfoMapper.update(updateOrderInfo, wrapper);
        if(row == 1) {
            return true;
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
    }
}
