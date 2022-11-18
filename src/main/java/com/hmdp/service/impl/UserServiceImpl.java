package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.EmailSendUtil;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    EmailSendUtil emailSendUtil;
/*    @Autowired
    UserMapper userMapper;*/

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //校验手机号
        if(RegexUtils.isEmailInvalid(phone)){
            //如果不符合返回错误信息
            return Result.fail("邮箱格式错误");
        }
        //符合，生成验证码
        val code = RandomUtil.randomNumbers(6);
        //保存，验证码
        session.setAttribute("code",code);
        //邮箱发送验证码
        val eMail = emailSendUtil.sendEMail(phone, "wql给你的验证码", code);
        if (!eMail){
            return Result.fail("发送失败");
        }
        //返回ok
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //校验手机号
        if(RegexUtils.isEmailInvalid(loginForm.getPhone())){
            return Result.fail("手机号格式错误");
        }
        //校验验证码
        String code = (String)session.getAttribute("code");
        if (!loginForm.getCode().equals(code)||code==null){
            return Result.fail("你输入的验证码错误");
        }
        //根据手机号查询用户数据
        User user = query().eq("phone", loginForm.getPhone()).one();
        //判断用户是否存在
        if (user==null){
            //不存在创建用户并保存
             user=createUserWithPhone(loginForm.getPhone());
        }
        //将user存进session
        session.setAttribute("user",user);
        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        //创建user
        User user=new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
