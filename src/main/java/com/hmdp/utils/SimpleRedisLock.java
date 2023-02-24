package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;


import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName SimpleRedisLock
 * @Description TODO
 * @Author WeiQiangLong
 * @Date 2023/2/8 15:37
 * @Version 1.0
 */
public class SimpleRedisLock implements ILock{

    private String name;
    private StringRedisTemplate redisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate redisTemplate) {
        this.name = name;
        this.redisTemplate = redisTemplate;
    }

    private static final String Key_PreFix="Lock:";
    private static final String ID_PreFix= UUID.randomUUID().toString(true);
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT=new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        //获取线程标识
        String threadId = ID_PreFix+ Thread.currentThread().getId();
        //获取锁
        Boolean ifSuccess = redisTemplate.opsForValue().setIfAbsent(Key_PreFix + name, threadId, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(ifSuccess);
    }

    @Override
    public void unLock() {
        //调用lua脚本
        redisTemplate.execute(UNLOCK_SCRIPT,
                Collections.singletonList(Key_PreFix + name),
                ID_PreFix+ Thread.currentThread().getId()
                );

    }
/*    @Override
    public void unLock() {
//        获取标识
        String threadId = ID_PreFix+ Thread.currentThread().getId();
//        获取redis里的标识
        String id = redisTemplate.opsForValue().get(Key_PreFix + name);
//        判断标识是否一致
        if(threadId.equals(id)) {
            //释放锁
            redisTemplate.delete(Key_PreFix + name);
        }
    }*/
}
