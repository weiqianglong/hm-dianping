package com.hmdp.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailSendUtil {

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")//获取yaml文件的配置
    private String myEmail;//我的发件邮箱

    /**
     * 发送邮件
     * @param toEmail 收件邮箱
     * @param subject 主题（信封标题）
     * @param content 信封内容
     * @return
     */
    public boolean sendEMail(String toEmail, String subject, String content){
        //获取信封
        SimpleMailMessage simpleMailMessage=new SimpleMailMessage();
        //发件人
        simpleMailMessage.setFrom(myEmail);
        //收件人
        simpleMailMessage.setTo(toEmail);
        //设置主题
        simpleMailMessage.setSubject(subject);
        //设置发件内容
        simpleMailMessage.setText(content);
        try {
            //发送邮件
            javaMailSender.send(simpleMailMessage);
            return true;
        }
        catch (Exception e){
            e.printStackTrace();
        }
    return false;
    }
}
