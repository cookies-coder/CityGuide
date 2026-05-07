package com.city.guide.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.city.guide.dto.Result;
import com.city.guide.dto.UserDTO;
import com.city.guide.entity.LimitedPerformanceTicket;
import com.city.guide.entity.TicketOrder;
import com.city.guide.mapper.TicketOrderMapper;
import com.city.guide.service.ILimitedPerformanceTicketService;
import com.city.guide.service.ITicketOrderService;
import com.city.guide.utils.RedisIdWorker;
import com.city.guide.utils.SimpleRedisLock;
import com.city.guide.utils.TravelerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

import static com.city.guide.utils.RedisConstants.LIMITED_PERFORMANCE_STOCK_KEY;

/**
 * <p>
 * 门票订单服务实现类
 * </p>
 *
 * @Cookie-coder
 * 
 */
@Slf4j
@Service
public class TicketOrderServiceImpl extends ServiceImpl<TicketOrderMapper, TicketOrder> implements ITicketOrderService {
    @Resource
    private ILimitedPerformanceTicketService limitedPerformanceTicketService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result seckillTicket(Long ticketId) {
        // 1. 查询限定表演票信息
        LimitedPerformanceTicket limitedPerformanceTicket = limitedPerformanceTicketService.getById(ticketId);
        if (limitedPerformanceTicket == null) {
            return Result.fail("门票不存在");
        }

        // 2. 判断秒杀是否开始
        LocalDateTime beginTime = limitedPerformanceTicket.getBeginTime();
        if (beginTime != null && beginTime.isAfter(LocalDateTime.now())) {
            return Result.fail("抢票尚未开始");
        }

        // 3. 判断秒杀是否结束
        LocalDateTime endTime = limitedPerformanceTicket.getEndTime();
        if (endTime != null && endTime.isBefore(LocalDateTime.now())) {
            return Result.fail("抢票已经结束");
        }

        // 4. 判断库存是否充足
        if (limitedPerformanceTicket.getStock() <= 0) {
            return Result.fail("库存不足");
        }

        // 5. 一人一单判断
        Long userId = TravelerContext.getTraveler().getId();
        //创建锁对象
        SimpleRedisLock lock = new SimpleRedisLock("cg:user:order:" + userId, stringRedisTemplate);
        //获取锁
        boolean isLocked = lock.tryLock(1200);
        if (!isLocked) {
            return Result.fail("用户已经购买过该门票");
        }
        try {
            Integer count = lambdaQuery()
                    .eq(TicketOrder::getUserId, userId)
                    .eq(TicketOrder::getTicketId, ticketId)
                    .count();
            if (count > 0) {
                return Result.fail("用户已经购买过该门票");
            }
        } finally {
            //释放锁
            lock.unlock();
        }

        // 6. 扣减库存
        String stockKey = LIMITED_PERFORMANCE_STOCK_KEY + ticketId;
        Boolean success = stringRedisTemplate.opsForValue().decrement(stockKey) >= 0;
        if (!success) {
            // 库存不足，恢复库存
            stringRedisTemplate.opsForValue().increment(stockKey);
            return Result.fail("库存不足");
        }

        // 7. 创建订单
        TicketOrder ticketOrder = createTicketOrder(userId, ticketId);
        
        log.info("用户抢票成功，用户ID: {}, 门票ID: {}, 订单ID: {}", userId, ticketId, ticketOrder.getId());
        return Result.ok(ticketOrder.getId());
    }

    @Transactional
    public TicketOrder createTicketOrder(Long userId, Long ticketId) {
        // 生成订单ID
        Long orderId = redisIdWorker.nextId("ticket_order");
        
        // 创建订单对象
        TicketOrder ticketOrder = new TicketOrder();
        ticketOrder.setId(orderId);
        ticketOrder.setUserId(userId);
        ticketOrder.setTicketId(ticketId);
        ticketOrder.setStatus(1); // 1：未支付
        ticketOrder.setCreateTime(LocalDateTime.now());
        ticketOrder.setUpdateTime(LocalDateTime.now());
        
        // 保存订单
        save(ticketOrder);
        
        return ticketOrder;
    }
}
