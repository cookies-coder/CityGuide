package com.city.guide.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.city.guide.dto.Result;
import com.city.guide.entity.Ticket;

/**
 * <p>
 * 门票服务类
 * </p>
 *
 * @Cookie-coder
 * 
 */
public interface ITicketService extends IService<Ticket> {

    Result queryTicketOfSpot(Long spotId);

    void addLimitedOfferTicket(Ticket ticket);
}
