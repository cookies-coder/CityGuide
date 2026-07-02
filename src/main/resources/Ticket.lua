--门票id
local TicketId=ARGV[1]
--用户id
local UserId=ARGV[2]
--2.1库存key
local StockKey="cg:limited_performance:stock:"..TicketId
--2.2订单key
local OrderKey="cg:limited_performance:order:"..TicketId
--3.脚本业务
--3.1判断库存是否充足
if(tonumber(redis.call("GET",StockKey)))<=0 then
    return 1
end
-- 3.2. 判断用户是否下单 SISMEMBER orderKey userId
if(redis.call('sismember', OrderKey, UserId) == 1) then
    -- 3.3. 存在，说明是重复下单，返回2
    return 2
end

-- 3.4. 扣库存 incrby stockKey -1
redis.call('incrby', StockKey, -1)

-- 3.5. 下单（保存用户） sadd orderKey userId
redis.call('sadd', OrderKey, UserId)

-- 3.6. 将订单消息写入Stream，供消费者异步创建订单
redis.call('xadd', 'cg:ticket:order:stream', '*', 'ticketId', TicketId, 'userId', UserId)
return 0