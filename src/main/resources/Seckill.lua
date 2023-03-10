-- 1.参数列表
-- 1.1.优惠券ID
local voucherId=ARGV[1]
-- 1.2 用户ID
local userId=ARGV[2]
-- 2.数据key
-- 2.1 库存key
local stockKey = 'seckill:stock:' .. voucherId
-- 2.2 订单key
local orderKey = 'seckill:order:' .. voucherId
-- 3.脚本业务
-- 3.1判断库存是否充足
--[[if (tonumber(redis.call('get',stockKey))<=0) then
    return 1
end]]
-- 3.2判断用户是否下单 SISMEMBER
if (redis.call('SISMEMBER',stockKey,userId)==1) then
--  3.3存在，说明重复下单
    return 2
end
-- 3.4 扣减库存 incrby
redis.call('incrby',stockKey,-1)
-- 3.5 下单（保存用户）sadd
redis.call('sadd',orderKey,userId)
return 0;
