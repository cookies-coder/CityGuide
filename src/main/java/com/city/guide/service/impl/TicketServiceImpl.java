package com.city.guide.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.city.guide.dto.Result;
import com.city.guide.entity.LimitedPerformanceTicket;
import com.city.guide.entity.Ticket;
import com.city.guide.mapper.TicketMapper;
import com.city.guide.service.ILimitedPerformanceTicketService;
import com.city.guide.service.ITicketService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

import static com.city.guide.utils.RedisConstants.LIMITED_PERFORMANCE_STOCK_KEY;

/**
 * <p>
 * 门票服务实现类
 * </p>
 *
 * @Cookie-coder
 * 
 */
@Service
public class TicketServiceImpl extends ServiceImpl<TicketMapper, Ticket> implements ITicketService {

    @Resource
    private ILimitedPerformanceTicketService limitedPerformanceTicketService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTicketOfSpot(Long spotId) {
        // 查询门票信息
        List<Ticket> tickets = getBaseMapper().queryTicketOfSpot(spotId);
        // 返回结果
        return Result.ok(tickets);
    }

    @Override
    @Transactional
    public void addLimitedPerformanceTicket(Ticket ticket) {
        // 保存门票
        save(ticket);
        // 保存限定表演票信息
        LimitedPerformanceTicket limitedPerformanceTicket = new LimitedPerformanceTicket();
        limitedPerformanceTicket.setTicketId(ticket.getId());
        limitedPerformanceTicket.setStock(ticket.getStock());
        limitedPerformanceTicket.setBeginTime(ticket.getBeginTime());
        limitedPerformanceTicket.setEndTime(ticket.getEndTime());
        limitedPerformanceTicketService.save(limitedPerformanceTicket);
        // 保存限定表演票库存到Redis中
        stringRedisTemplate.opsForValue().set(LIMITED_PERFORMANCE_STOCK_KEY + ticket.getId(), ticket.getStock().toString());
    }
}
