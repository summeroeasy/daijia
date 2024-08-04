package com.atguigu.daijia.dispatch.service;

import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;

import java.util.List;

public interface NewOrderService {

    /**
     * 添加并创建新任务
     * @param newOrderTaskVo 新订单任务
     * @return 任务id
     */
    Long addAndStartTask(NewOrderTaskVo newOrderTaskVo);

    /**
     * 执行任务
     * @param jobId 任务id
     */
    void executeTask(long jobId);

    /**
     * 查询司机新订单数据
     * @param driverId 司机id
     * @return 新订单数据
     */
    List<NewOrderDataVo> findNewOrderQueueData(Long driverId);

    /**
     * 清空司机队列数据
     * @param driverId 司机id
     * @return 清除结果
     */
    Boolean clearNewOrderQueueData(Long driverId);
}
