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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
  @Test
  void test1(){
     int[] a={12,14,4,25,27,15,10};
     int temp=0;
      for (int i = 0; i < a.length; i++) {
          for (int j = 0; j < a.length-1-i; j++) {
              if(a[j]>a[j+1]){
                 temp= a[j+1];
                 a[j+1]=a[j];
                 a[j]=temp;
              }
          }
      }
      Arrays.stream(a).forEach(value -> System.out.println(value));
  }
  @Test
    void test2(){
      Map<String,Object> map=new HashMap();
      map.put("1","1");
      map.put("2","2");
      map.put("3","3");
      map.put("4","4");
      map.put("5","5");
      for (Map.Entry<String,Object> entry : map.entrySet()) {
          System.out.println("Key:"+entry.getKey()+",value"+entry.getValue());
      }
  }

}
