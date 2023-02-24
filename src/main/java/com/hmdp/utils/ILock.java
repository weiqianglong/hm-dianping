package com.hmdp.utils;

/**
 * @ClassName ILock
 * @Description TODO
 * @Author WeiQiangLong
 * @Date 2023/2/8 15:32
 * @Version 1.0
 */
public interface ILock {
    /*
    * 尝试获取锁
    * @param timeoutSec 过期时间
    * @return true代表获取成功，false代表获取失败
    * */
    boolean tryLock(long timeoutSec);

    /*
    * 释放锁
    * */
    void unLock();
}
