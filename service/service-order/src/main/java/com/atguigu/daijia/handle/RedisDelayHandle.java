package com.atguigu.daijia.handle;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.order.service.OrderInfoService;
import jakarta.annotation.PostConstruct;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RedisDelayHandle {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private OrderInfoService orderInfoService;

    @PostConstruct
    public void listener() {
        new Thread(() -> {
            while (true){
                // 获取延迟队列里面阻塞队列
                RBlockingDeque<String> blockingDeque = redissonClient.getBlockingDeque("queue_cancel");

                try {
                    //从队列获取消息
                    String orderId = blockingDeque.take();

                    // 取消订单
                    if (StringUtils.hasText(orderId)){
                        orderInfoService.orderCancel(Long.parseLong(orderId));
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }
}
