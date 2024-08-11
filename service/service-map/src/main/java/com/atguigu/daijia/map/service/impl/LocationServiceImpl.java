package com.atguigu.daijia.map.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.map.service.LocationService;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationServiceImpl implements LocationService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DriverInfoFeignClient driverInfoFeignClient;

    /**
     * 开启接单服务：更新司机经纬度位置
     *
     * @param updateDriverLocationForm
     * @return
     */
    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {

        Point point = new Point(updateDriverLocationForm.getLongitude().doubleValue(),
                updateDriverLocationForm.getLatitude().doubleValue());
        redisTemplate.opsForGeo().add(RedisConstant.DRIVER_GEO_LOCATION, point, updateDriverLocationForm.getDriverId().toString());
        return true;
    }

    /**
     * 移除司机位置信息
     *
     * @param driverId
     * @return
     */
    @Override
    public Boolean removeDriverLocation(Long driverId) {
        redisTemplate.opsForGeo().remove(RedisConstant.DRIVER_GEO_LOCATION, driverId.toString());
        return true;
    }

    /**
     * 查询附近满足条件的司机
     *
     * @param searchNearByDriverForm
     * @return
     */
    @Override
    public List<NearByDriverVo> searchNearByDriver(SearchNearByDriverForm searchNearByDriverForm) {
        //搜索位置经纬度位置5公里以内的司机
        //1 操作redis里geo对象
        //创建point,设置经纬度
        Point point = new Point(searchNearByDriverForm.getLongitude().doubleValue(),
                searchNearByDriverForm.getLatitude().doubleValue());
        //定义距离 5公里

//        Distance distance = new Distance(200,
//                RedisGeoCommands.DistanceUnit.KILOMETERS);
        Distance distance = new Distance(SystemConstant.NEARBY_DRIVER_RADIUS,
                RedisGeoCommands.DistanceUnit.KILOMETERS);
        //创建circle对象
        Circle circle = new Circle(point, distance);
        //定义GEO参数,设置返回结果包含内容
        RedisGeoCommands.GeoRadiusCommandArgs args =
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                        .includeDistance()  //包含距离
                        .includeCoordinates() //包含坐标
                        .sortAscending(); //升序

        GeoResults<RedisGeoCommands.GeoLocation<String>> result =
                redisTemplate.opsForGeo().radius(RedisConstant.DRIVER_GEO_LOCATION, circle, args);

        //2 查询redis最终返回list集合
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = result.getContent();
        //3 对查询list集合进行处理
        // 遍历list集合 得到每个司机信息
        // 根据每个司机个性化设置信息判断
        List<NearByDriverVo> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(content)) {
            Iterator<GeoResult<RedisGeoCommands.GeoLocation<String>>> iterator = content.iterator();
            while (iterator.hasNext()) {
                GeoResult<RedisGeoCommands.GeoLocation<String>> item = iterator.next();
                //获取司机id
                Long driverId = Long.parseLong(item.getContent().getName());

                //远程调用,根据司机id个性化设置信息
                DriverSet driverSet = driverInfoFeignClient.getDriverSet(driverId).getData();
                //判断订单里程order_distance
                BigDecimal orderDistance = driverSet.getOrderDistance();
                //orderDistance 司机个人设置能接受的订单里程
                // 如果不等于0 ，比如30，接单30公里代驾订单。
                //接单距离 - 当前单子距离  < 0,不复合条件
                //searchNearByDriverForm 前端传递过来的预估里程
                if (orderDistance.doubleValue() != 0 && orderDistance.subtract(searchNearByDriverForm.getMileageDistance()).doubleValue() < 0) {
                    continue;
                }

                //判断接单里程accept_distance
                //当前接单距离 acceptDistance可以接受距离下单人的里程
                BigDecimal currentDistance = new BigDecimal(item.getDistance().getValue()).setScale(2, BigDecimal.ROUND_HALF_UP);
                BigDecimal acceptDistance = driverSet.getAcceptDistance();
                if (acceptDistance.doubleValue() != 0 && currentDistance.subtract(acceptDistance).doubleValue() > 0) {
                    continue;
                }
                //封装复合条件数据
                NearByDriverVo nearByDriverVo = new NearByDriverVo();
                nearByDriverVo.setDriverId(driverId);
                nearByDriverVo.setDistance(currentDistance);
                list.add(nearByDriverVo);
            }

        }
        return list;
    }

    //更新司机位置到redis缓存当中
    @Override
    public Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm) {
        OrderLocationVo orderLocationVo = new OrderLocationVo();
        orderLocationVo.setLatitude(updateOrderLocationForm.getLatitude());
        orderLocationVo.setLongitude(updateOrderLocationForm.getLongitude());

        redisTemplate.opsForValue().set(RedisConstant.UPDATE_ORDER_LOCATION + updateOrderLocationForm.getOrderId(),
                orderLocationVo);
        return true;
    }
}
