package com.city.guide.controller;


import com.city.guide.dto.Result;
import com.city.guide.service.ITicketOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>
 * 门票订单前端控制器
 * </p>
 *
 * @Cookie-coder
 * 
 */
@RestController
@RequestMapping("/ticket-order")
public class TicketOrderController {
    
    @Resource
    private ITicketOrderService ticketOrderService;
    
    /**
     * 抢限定表演票
     * @param ticketId 门票id
     * @return 订单id
     */
    @PostMapping("limited-performance/{id}")
    public Result seckillTicket(@PathVariable("id") Long ticketId) {
        return ticketOrderService.seckillTicket(ticketId);
    }

    /**
     * 轮询查询秒杀下单结果
     * @param ticketId 门票id
     * @return 订单id或处理状态
     */
    @GetMapping("limited-performance/result/{id}")
    public Result querySeckillResult(@PathVariable("id") Long ticketId) {
        return ticketOrderService.querySeckillResult(ticketId);
    }
}
