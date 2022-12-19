package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Autowired
    StringRedisTemplate redisTemplate;
    @Resource
    CacheClient cacheClient;
    @Override
    public Result queryByID(Long id) {
        // 1.从redis查询商铺缓存
        // 缓存穿透
        //Shop shop = queryWithPassThrough(id);
//         缓存击穿
//        Shop shop = queryWithMutex(id);
/*        // 缓存击穿,逻辑过期
        Shop shop = queryWithLogicalExpire(id);*/
        // 工具类实现缓存穿透
        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
/*        // 工具类实现缓存击穿逻辑过期
        Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);*/
        if(shop==null){
            return Result.fail("店铺不存在!");
        }
        return Result.ok(shop);
    }

    /*
     * 缓存击穿
     * */
    public Shop queryWithMutex(Long id){
        // 1.从redis查询商铺缓存
        String shopJson = redisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 3.存在，返回数据
            Shop shop = JSONUtil.toBean(shopJson,Shop.class);
            return shop;
        }
        if (shopJson!=null){
            return null;
        }
        //4 实现缓存重建
        //4.1 获取互斥锁
        String lockKey="LOCK:SHOP:"+id;
        Shop shop1 = null;
        try {
            boolean flag = tryLock(lockKey);
            //4.2 判断获取互斥锁是否成功
            if (flag==false) {
                //4.3 失败，休眠重试
                Thread.sleep(50);
                return  queryWithMutex(id);
            }
            //4.4 成功，根据id查询数据库
            shop1 = getById(id);
            //模拟重建的延迟
            Thread.sleep(200);
            // 5.不存在，返回错误
            if (shop1==null){
                //将空值写入redis
                redisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,"");
                redisTemplate.expire(CACHE_SHOP_KEY+id,CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            String jsonStr = JSONUtil.toJsonStr(shop1);
            redisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,jsonStr);
            redisTemplate.expire(CACHE_SHOP_KEY+id,CACHE_SHOP_TTL, TimeUnit.MINUTES);
            // 6.释放互斥锁
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unLock(lockKey);
        }
        // 7.返回数据
        return shop1;
    }
    /*
    * 缓存穿透
    * */
    public Shop queryWithPassThrough(Long id){
        // 1.从redis查询商铺缓存
        String shopJson = redisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 3.存在，返回数据
            Shop shop = JSONUtil.toBean(shopJson,Shop.class);
            return shop;
        }
        if (shopJson!=null){
            return null;
        }
        // 4.不存在，根据id查询数据库
        Shop shop1 = getById(id);
        // 5.不存在，返回错误
        if (shop1==null){
            //将空值写入redis
            redisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,"");
            redisTemplate.expire(CACHE_SHOP_KEY+id,CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 6.存在，写入redis
/*        Map<String, Object> shopMap1 = BeanUtil.beanToMap(shop1,new HashMap<>(), CopyOptions.create()
                .setIgnoreNullValue(true)
                .setFieldValueEditor((fieldName,fieldValue)->{
                    if(fieldValue==null){
                        fieldValue="0";
                    }
                    else {
                       fieldValue= fieldValue.toString();
                    }
                    return fieldValue;
                        }
                ));*/
        String jsonStr = JSONUtil.toJsonStr(shop1);
        redisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,jsonStr);
        redisTemplate.expire(CACHE_SHOP_KEY+id,CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 7.返回数据
        return shop1;
    }

    private static final ExecutorService CACHE_REBULID_EXECUTOR = Executors.newFixedThreadPool(10);
    /*
     * 缓存击穿逻辑过期
     * */
    public Shop queryWithLogicalExpire(Long id){
        // 1.从redis查询商铺缓存
        String shopJson = redisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
        // 2.判断是否存在
        if (StrUtil.isBlank(shopJson)) {
            // 3.不存在，返回数据
            return null;
        }
        // 4.命中，先把redis数据反序列化
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        JSONObject data = (JSONObject) redisData.getData();
        Shop shop = JSONUtil.toBean(data, Shop.class);
        // 5.判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())){
            //5.1.未过期，直接返回店铺信息
            return shop;
        }
        //5.2.已过期，需要重建缓存
        //6.缓存重建
//        6.1获取互斥锁
        String lockKey="LOCK:SHOP:"+id;
        boolean flag = tryLock(lockKey);
//        6.2.获取失败返回店铺信息
        if (flag){
            String shopJson1 = redisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
            if (JSONUtil.toBean(shopJson, RedisData.class).getExpireTime().isAfter(LocalDateTime.now())){
                return shop;
            }
            CACHE_REBULID_EXECUTOR.submit(()->{
                try {
                    saveShop2Redis(id,20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unLock(lockKey);
                }
            });
        }
        return shop;
    }
    public void saveShop2Redis(Long id,Long expireSeconds) throws InterruptedException {
        //1.查询店铺数据
        Shop shop = getById(id);
        Thread.sleep(200);
        //2.封装逻辑过期时间
        RedisData redisData=new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //3.写入redis
        redisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }

    @Override
    @Transactional
    public Result updateShop(Shop shop) {
        //判断id是否存在
        Long shopId = shop.getId();
        if (shopId==null){
            return Result.fail("店铺id不能为空");
        }
        //1.更新数据库
        updateById(shop);
        //2.删除缓存
        redisTemplate.delete(CACHE_SHOP_KEY+shopId);
        return Result.ok();
    }

    private boolean tryLock(String key){
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private  boolean unLock(String key){
        Boolean flag = redisTemplate.delete(key);
        return BooleanUtil.isTrue(flag);
    }
}
