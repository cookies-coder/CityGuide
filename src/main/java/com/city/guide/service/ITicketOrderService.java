package com.city.guide.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.city.guide.dto.Result;
import com.city.guide.entity.TicketOrder;

/**
 * <p>
 * 门票订单服务类
 * </p>
 *
 * @Cookie-coder
 * 
 */
public interface ITicketOrderService extends IService<TicketOrder> {

    Result seckillTicket(Long ticketId);

    /**
     * 前端轮询查询秒杀下单结果
     * @param ticketId 门票id
     * @return 订单id或处理状态
     */
    Result querySeckillResult(Long ticketId);
}
