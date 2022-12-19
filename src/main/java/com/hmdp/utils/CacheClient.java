package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

/**
 * @ClassName CacheClient
 * @Description TODO
 * @Author WeiQiangLong
 * @Date 2022/12/5 15:48
 * @Version 1.0
 */
@Component
@Slf4j
public class CacheClient {
    @Autowired
    private final StringRedisTemplate redisTemplate;

    public CacheClient(StringRedisTemplate redisTemplate){
        this.redisTemplate=redisTemplate;
    }
    public void set(String key, Object value, Long time, TimeUnit unit){
        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }
    /*
    * 设置逻辑过期
    * */
    public void setWithLogicExpire(String key, Object value, Long time, TimeUnit unit){
        RedisData redisData=new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    public<R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID,R>dbFallBack,Long time,TimeUnit unit){
        // 1.从redis查询商铺缓存
        String key=keyPrefix+id;
        String Json = redisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(Json)) {
            // 3.存在，返回数据
            R r = JSONUtil.toBean(Json,type);
            return r;
        }
        if (Json!=null){
            return null;
        }
        // 4.不存在，根据id查询数据库

        R r = dbFallBack.apply(id);
        // 5.不存在，返回错误
        if (r==null){
            //将空值写入redis
            redisTemplate.opsForValue().set(key,"");
            redisTemplate.expire(key,time, unit);
            return null;
        }
        // 6.存在，写入redis
            this.set(key,r,time,unit);
        // 7.返回数据
        return r;
    }
    private static final ExecutorService CACHE_REBULID_EXECUTOR = Executors.newFixedThreadPool(10);
    /*
     * 缓存击穿逻辑过期
     * */
    public <R,T>R queryWithLogicalExpire(String keyPrefix,T id,Class<R> type,Function<T,R>dbFallBack,Long time,TimeUnit unit){
        // 1.从redis查询商铺缓存
        String key=keyPrefix+id;
        String Json = redisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isBlank(Json)) {
            // 3.不存在，返回数据
            return null;
        }
        // 4.命中，先把redis数据反序列化
        RedisData redisData = JSONUtil.toBean(Json, RedisData.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        JSONObject data = (JSONObject) redisData.getData();
        R r = JSONUtil.toBean(data, type);
        // 5.判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())){
            //5.1.未过期，直接返回店铺信息
            return r;
        }
        //5.2.已过期，需要重建缓存
        //6.缓存重建
//        6.1获取互斥锁
        String lockKey="LOCK:SHOP:"+id;
        boolean flag = tryLock(lockKey);
//        6.2.获取失败返回店铺信息
        if (flag){
            String shopJson1 = redisTemplate.opsForValue().get(key);
            if (JSONUtil.toBean(Json, RedisData.class).getExpireTime().isAfter(LocalDateTime.now())){
                return r;
            }
            CACHE_REBULID_EXECUTOR.submit(()->{
                try {
                    saveShop2Redis(dbFallBack.apply(id),20L,key);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unLock(lockKey);
                }
            });
        }
        return r;
    }

    private boolean tryLock(String key){
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private  boolean unLock(String key){
        Boolean flag = redisTemplate.delete(key);
        return BooleanUtil.isTrue(flag);
    }

    public void saveShop2Redis(Object r,Long expireSeconds,String key) throws InterruptedException {
        //1.查询店铺数据
        Thread.sleep(200);
        //2.封装逻辑过期时间
        RedisData redisData=new RedisData();
        redisData.setData(r);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //3.写入redis
        redisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }
}
