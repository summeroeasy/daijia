package com.atguigu.daijia.map.repository;

import com.atguigu.daijia.model.entity.map.OrderServiceLocation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderServiceLocationRepository extends MongoRepository<OrderServiceLocation, String> {

    /**
     * 根据订单id查询订单服务位置并根据时间排序
     * @param orderId
     * @return
     */
    List<OrderServiceLocation> findByOrderIdOrderByCreateTimeAsc(Long orderId);
}
