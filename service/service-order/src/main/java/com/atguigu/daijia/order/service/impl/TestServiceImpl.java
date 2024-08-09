package com.atguigu.daijia.order.service.impl;

import com.atguigu.daijia.order.service.TestService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TestServiceImpl implements TestService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    // 测试本地分布式锁
    @Override
    public synchronized void testLock() {
        // 查询Redis中的num值
        String value = (String) this.redisTemplate.opsForValue().get("num");
        // 没有该值return
        if (StringUtils.isBlank(value)) {
            return;
        }
        // 有值就转成成int
        int num = Integer.parseInt(value);
        // 把Redis中的num值+1
        this.redisTemplate.opsForValue().set("num", String.valueOf(--num));
    }

    // 测试本地分布式锁
    @Override
    public synchronized void testLock1() {
        //从Redis里面获取数据
        //获取当前锁
        //1 获取当前锁
        //Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("lock", "lock");
        //添加时间防止死锁
        Boolean isifAbsent = redisTemplate.opsForValue()
                .setIfAbsent("lock", "lock", 3, TimeUnit.SECONDS);
        //2 如果获取到锁，，从redis里面获取数据 数据+1 放回redis里面
        if (isifAbsent) {
            // 查询Redis中的num值
            String value = (String) this.redisTemplate.opsForValue().get("num");
            // 没有该值return
            if (StringUtils.isBlank(value)) {
                return;
            }
            // 有值就转成成int
            int num = Integer.parseInt(value);
            // 把Redis中的num值+1
            this.redisTemplate.opsForValue().set("num", String.valueOf(--num));
            //3 释放锁
            redisTemplate.delete("lock");
        } else {
            try {
                Thread.sleep(100);
                this.testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // 使用UUID测试本地分布式锁
    @Override
    public synchronized void testLock2() {
        // 生成UUID
        String uuid = UUID.randomUUID().toString();
        //从Redis里面获取数据
        //获取当前锁
        //1 获取当前锁
        //Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("lock", "lock");
        //添加时间防止死锁
        Boolean isifAbsent = redisTemplate.opsForValue()
                .setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS);
        //2 如果获取到锁，，从redis里面获取数据 数据+1 放回redis里面
        if (isifAbsent) {
            // 查询Redis中的num值
            String value = (String) this.redisTemplate.opsForValue().get("num");
            // 没有该值return
            if (StringUtils.isBlank(value)) {
                return;
            }
            // 有值就转成成int
            int num = Integer.parseInt(value);
            // 把Redis中的num值+1
            this.redisTemplate.opsForValue().set("num", String.valueOf(--num));
            //3 释放锁
            if (uuid.equals(redisTemplate.opsForValue().get("lock"))) {
                redisTemplate.delete("lock");
            }
        } else {
            try {
                Thread.sleep(100);
                this.testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // 使用UUID测试本地分布式锁 + lua脚本保证原子性
    @Override
    public synchronized void testLock3() {
        // 生成UUID
        String uuid = UUID.randomUUID().toString();
        //从Redis里面获取数据
        //获取当前锁
        //1 获取当前锁
        //Boolean ifAbsent = redisTemplate.opsForValue().setIfAbsent("lock", "lock");
        //添加时间防止死锁
        Boolean isifAbsent = redisTemplate.opsForValue()
                .setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS);
        //2 如果获取到锁，，从redis里面获取数据 数据+1 放回redis里面
        if (isifAbsent) {
            // 查询Redis中的num值
            String value = (String) this.redisTemplate.opsForValue().get("num");
            // 没有该值return
            if (StringUtils.isBlank(value)) {
                return;
            }
            // 有值就转成成int
            int num = Integer.parseInt(value);
            // 把Redis中的num值+1
            this.redisTemplate.opsForValue().set("num", String.valueOf(--num));
            //3 释放锁 Lua脚本实现
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript();
            //lua脚本
            String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                    "then\n" +
                    "    return redis.call(\"del\",KEYS[1])\n" +
                    "else\n" +
                    "    return 0\n" +
                    "end";
            redisScript.setScriptText(script);
            redisScript.setResultType(Long.class);
            redisTemplate.execute(redisScript, Arrays.asList("lock"), uuid);
        } else {
            try {
                Thread.sleep(100);
                this.testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // 使用Redisson测试分布式锁
    @Override
    public synchronized void testLock4() throws InterruptedException {

        //通过Redisson创建锁对象
        RLock lock = redissonClient.getLock("lock1");

        // 尝试获取锁
        //1 lock.lock得到锁
        //阻塞等待直到获取到锁,获取到锁后，有一个默认的锁过期时间，默认是30s
        lock.lock();
//        // 编写业务代码
//        // 2 获取到了锁，锁过期时间为10秒
//        lock.lock(10, TimeUnit.SECONDS);
//        //3 第一个参数获取锁的等待时间 第二个参数锁过期时间 第三个参数时间单位
//        boolean tryLock = lock.tryLock(30, 10, TimeUnit.SECONDS);


        String value = (String) this.redisTemplate.opsForValue().get("num");
        // 没有该值return
        if (StringUtils.isBlank(value)) {
            return;
        }
        // 有值就转成成int
        int num = Integer.parseInt(value);
        // 把Redis中的num值+1
        this.redisTemplate.opsForValue().set("num", String.valueOf(--num));

        // 释放锁
        lock.unlock();

    }
}