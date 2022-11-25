package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RefreshTokenInterceptor implements HandlerInterceptor {
    private StringRedisTemplate redisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    /*        //1.获取session
        //HttpSession session = request.getSession();*/
        //获取请求头中的token
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)){
            return true;
        }
/*        //2.获取session中的用户
        Object user = session.getAttribute("user");*/
        //基于token获取redis中的用户信息
        String key= RedisConstants.LOGIN_USER_KEY+token;
        Map<Object, Object> user = redisTemplate.opsForHash().entries(key);
        //3.判断redis中的用户是否存在
        if (user==null) {
            return true;
        }
        //将查询到的hash数据转为UserDTO对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(user, new UserDTO(), false);
        //5.存在，保存用户信息早threadlocal
        UserHolder.saveUser(userDTO);
        //刷新token有效期
        redisTemplate.expire(key,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;
    }
}
