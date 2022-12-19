package com.hmdp.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @ClassName RedisIDWorker
 * @Description TODO
 * @Author WeiQiangLong
 * @Date 2022/12/7 10:02
 * @Version 1.0
 */
@Component
public class RedisIDWorker {
    private static final long BEGIN_TIMESTAMP=1640995200;
    /*
    *序列号的位数
     */
    private static final int COUNT_BITS=32;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public long nextID(String keyPreFix) {
        // 1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp = nowSecond - BEGIN_TIMESTAMP;
        // 2.生成序列号
        // 2.1.获取到当前日期，精确到天
        String nowDate = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 2.2.自增长
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPreFix + ":" + nowDate);
        // 3.拼接并返回
        return timeStamp << COUNT_BITS | count;
    }

}
