package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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
    @Override
    public Result queryByID(Long id) {
        // 1.从redis查询商铺缓存
        Map<Object, Object> shopMap = redisTemplate.opsForHash().entries(RedisConstants.CACHE_SHOP_KEY+id);
        // 2.判断是否存在
        if (shopMap.size()>0) {
            // 3.存在，返回数据
            Shop shop = BeanUtil.fillBeanWithMap(shopMap, new Shop(), false);
            return Result.ok(shop);
        }
        // 4.不存在，根据id查询数据库
        Shop shop1 = getById(id);
        // 5.不存在，返回错误
        if (shop1==null){
            return Result.fail("数据不存在");
        }
        // 6.存在，写入redis
        Map<String, Object> shopMap1 = BeanUtil.beanToMap(shop1,new HashMap<>(), CopyOptions.create()
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
                ));
        redisTemplate.opsForHash().putAll(RedisConstants.CACHE_SHOP_KEY+id,shopMap1);
        // 7.返回数据
        return Result.ok(shop1);
    }
}
