package com.hmdp;

import cn.hutool.core.util.RandomUtil;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.EmailSendUtil;
import com.hmdp.utils.RedisIDWorker;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class HmDianPingApplicationTests {

    @Autowired
    EmailSendUtil emailSendUtil;
    @Resource
    ShopServiceImpl service;
    @Autowired
    RedisIDWorker redisIDWorker;
    private ExecutorService es= Executors.newFixedThreadPool(500);
 @Test
    void mailTest(){
     val s = RandomUtil.randomNumbers(6);
     emailSendUtil.sendEMail("474288124@qq.com","验证码","你的验证码是："+s);
 }
 @Test
    void shopTest() throws InterruptedException {
     service.saveShop2Redis(1l,10L);
    }
  @Test
    void IdWorkerTest() throws InterruptedException {
      CountDownLatch countDownLatch = new CountDownLatch(300);
      Runnable task=()->{
         for (int i = 0; i < 100; i++) {
             long order = redisIDWorker.nextID("order");
             System.out.println("id="+order);
         }
         countDownLatch.countDown();
      };
      long l = System.currentTimeMillis();
      for (int i = 0; i < 300; i++) {
          es.submit(task);
      }
      countDownLatch.await();
      long l1 = System.currentTimeMillis();
      System.out.println("time="+(l1-l));
  }
}
