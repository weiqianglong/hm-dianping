package com.hmdp;

import cn.hutool.core.util.RandomUtil;
import com.hmdp.utils.EmailSendUtil;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HmDianPingApplicationTests {

    @Autowired
    EmailSendUtil emailSendUtil;
 @Test
    void mailTest(){
     val s = RandomUtil.randomNumbers(6);
     emailSendUtil.sendEMail("474288124@qq.com","验证码","你的验证码是："+s);
 }

}
