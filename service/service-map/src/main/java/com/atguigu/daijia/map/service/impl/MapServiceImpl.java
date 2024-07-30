package com.atguigu.daijia.map.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.map.service.MapService;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class MapServiceImpl implements MapService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("tencent.cloud.map")
    private String key;

    /**
     * 计算驾驶路线
     *
     * @param calculateDrivingLineForm
     * @return
     */
    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        //定义调用腾讯地址
        String url = "https://apis.map.qq.com/ws/direction/v1/driving/?from={from}&to={to}&key={key}";
        //封装需要传递的参数
        Map<String, String> map = new HashMap();
        //开始位置
        // 经纬度：比如 北纬40 东京116
        map.put("from", calculateDrivingLineForm.getStartPointLatitude() + "," + calculateDrivingLineForm.getStartPointLongitude());
        //结束位置
        map.put("to", calculateDrivingLineForm.getEndPointLatitude() + "," + calculateDrivingLineForm.getEndPointLongitude());
        //key
        map.put("key", key);
        //使用RestTemplate调用 GET
        JSONObject result = restTemplate.getForObject(url, JSONObject.class, map);
        //处理返回结果
        int status = result.getIntValue("status");
        if (status != 0) {
            throw new GuiguException(ResultCodeEnum.MAP_FAIL);
        }
        //获取返回路线信息
        JSONObject routes = result.getJSONObject("result").getJSONArray("routes").getJSONObject(0);
        //创建vo对象
        DrivingLineVo drivingLineVo = new DrivingLineVo();
        //方案预估时间
        drivingLineVo.setDistance(routes.getBigDecimal("duration"));
        //方案距离
        // 设置行驶路线的距离，单位为千米
        // 通过从routes中获取distance值，将其转换为千米并保留两位小数来实现
        // 使用BigDecimal来确保计算过程的精确性，并采用HALF_UP舍入模式来处理四舍五入的情况
        drivingLineVo.setDistance(routes.getBigDecimal("distance")
                .divide(new BigDecimal(1000))
                .setScale(2, RoundingMode.HALF_UP));
        //路线
        drivingLineVo.setPolyline(routes.getJSONArray("polyline"));
        return drivingLineVo;
    }
}
