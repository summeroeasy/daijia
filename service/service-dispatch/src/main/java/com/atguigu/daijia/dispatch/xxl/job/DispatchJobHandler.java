package com.atguigu.daijia.dispatch.xxl.job;

import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

@Component
public class DispatchJobHandler {

    @XxlJob("FirstJobHandler")
    public void testJobHandler() {
        System.out.println("testJobHandler集成测试");
    }
}
