package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOPLIST_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    StringRedisTemplate redisTemplate;
    @Override
    public Result queryList() {
        //1.查询缓存
        String shopLsit = redisTemplate.opsForValue().get(CACHE_SHOPLIST_KEY);
        //2.存在返回数据
        if(StrUtil.isNotBlank(shopLsit)){
            JSONArray objects =JSONUtil.parseArray(shopLsit);
            List<ShopType> shopType = JSONUtil.toList(objects, ShopType.class);
            return Result.ok(shopType);
        }
        //3.不存在，查询数据库
        List<ShopType> list = query().list();
        //4.不存在，返回错误
        if (!(list.size()>0)){
           return Result.fail("数据不存在");
        }
        //5.存在，存入redis缓存
        String shopList = JSONUtil.toJsonStr(list);
        redisTemplate.opsForValue().set(CACHE_SHOPLIST_KEY,shopList);
        //返回结果
        return Result.ok(list);
    }
}
