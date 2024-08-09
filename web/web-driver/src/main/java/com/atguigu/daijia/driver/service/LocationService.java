package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;

public interface LocationService {


    /**
     * 更新司机经纬度位置
     * @param updateDriverLocationForm
     * @return
     */
    Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm);

    //更新司机位置到redis缓存中
    Object updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm);
}
