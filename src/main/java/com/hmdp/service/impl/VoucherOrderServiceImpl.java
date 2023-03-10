package com.hmdp.service.impl;

import cn.hutool.core.lang.UUID;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIDWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Resource
    private RedisIDWorker redisIDWorker;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT=new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("Seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }
    public Result setKillVoucher(Long voucherId) {
//        1.执行lua脚本
        Long userid= UserHolder.getUser().getId();
        Long result = redisTemplate.execute(SECKILL_SCRIPT,Collections.emptyList(),voucherId.toString(),userid.toString());
//        2.判断结果是否为0
        int r=result.intValue();
//        2.1 不为0代表没有购买资格
        if(r!=0){
            return Result.fail(r==1?"库存不足":"重复下单");
        }
//        2.2 为0代表有购买资格，把下单信息保存到阻塞队列
//        TODO 保存阻塞队列
        long orderId = redisIDWorker.nextID("order");
//        2.3 返回订单id
        return  Result.ok();
    }
/*    @Override
    public Result setKillVoucher(Long voucherId) {
        //1.查询优惠券
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        //2.判断优惠时间是否开始
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("秒杀未开始");
        }
        //3.判断秒杀是否已经结束
        if (!seckillVoucher.getEndTime().isAfter(LocalDateTime.now())){
            return Result.fail("秒杀已结束");
        }
        //4.判断库存是否充足
        if (seckillVoucher.getStock()<1){
            return Result.fail("库存不足");
        }
        Long userId = UserHolder.getUser().getId();
        //5.单体服务器加锁
    *//*    synchronized(userId.toString().intern()) {
            //直接调用事务失效，获取代理对象（事务）
//            IVoucherOrderService iVoucherOrderService = (IVoucherOrderService) AopContext.currentProxy();
            return iVoucherOrderService.createVoucherOrder(voucherId);
        }  *//*
        //5.多服务器分步式锁
        //5.1获取锁对象
       // SimpleRedisLock lock = new SimpleRedisLock("Order:" + userId, redisTemplate);
//        redisson调用锁
        RLock lock = redissonClient.getLock("LOCK:Order:" + userId);
        //5.2 获取锁
        boolean tryLock = lock.tryLock();
//        5.3判断获取锁是否成功
//        5.3.1失败
        if(!tryLock){
            return Result.fail("一个人只允许下一单");
        }
//        5.3.2成功
        try {
            //5.4直接调用事务失效，获取代理对象（事务）
            IVoucherOrderService iVoucherOrderService = (IVoucherOrderService) AopContext.currentProxy();
            return iVoucherOrderService.createVoucherOrder(voucherId);
        } finally {
//            5.5释放锁
            lock.unlock();
        }

    }*/

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        //5.一人一单
        Long userId = UserHolder.getUser().getId();
            //5.1查询订单
            Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
            //5.2判断是否存在
            if (count > 0) {
                return Result.fail("用户已经购买过了");
            }
            //6.扣减库存
            boolean success = seckillVoucherService
                    .update()
                    .setSql(" Stock = Stock-1 ")
                    .eq("voucher_id", voucherId)
                    .gt("stock", 0)
                    .update();
            if (!success) {
                //扣减失败
                return Result.fail("库存不足");
            }
            //7.创建订单
            VoucherOrder voucherOrder = new VoucherOrder();
            //7.1订单id
            long orderId = redisIDWorker.nextID("order");
            voucherOrder.setId(orderId);
            //7.2用户id

            voucherOrder.setUserId(userId);
            //7.3代金券id
            voucherOrder.setVoucherId(voucherId);
            //8.保存订单
            save(voucherOrder);
            //9.返回订单id
            return Result.ok(orderId);

    }
}
