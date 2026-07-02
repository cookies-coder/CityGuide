package com.city.guide.controller;


import com.city.guide.dto.Result;
import com.city.guide.entity.Ticket;
import com.city.guide.service.ITicketService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 门票前端控制器
 * </p>
 *
 * @Cookie-coder
 * 
 */
@RestController
@RequestMapping("/ticket")
public class TicketController {

    @Resource
    private ITicketService ticketService;

    /**
     * 新增普通门票
     * @param ticket 门票信息
     * @return 门票id
     */
    @PostMapping
    public Result addTicket(@RequestBody Ticket ticket) {
        ticketService.save(ticket);
        return Result.ok(ticket.getId());
    }

    /**
     * 新增限定表演票
     * @param ticket 门票信息，包含限定表演票信息
     * @return 门票id
     */
    @PostMapping("limited-performance")
    public Result addLimitedPerformanceTicket(@RequestBody Ticket ticket) {
        ticketService.addLimitedPerformanceTicket(ticket);
        return Result.ok(ticket.getId());
    }

    /**
     * 查询景点的门票列表
     * @param spotId 景点id
     * @return 门票列表
     */
    @GetMapping("/list/{spotId}")
    public Result queryTicketOfSpot(@PathVariable("spotId") Long spotId) {
       return ticketService.queryTicketOfSpot(spotId);
    }
}
