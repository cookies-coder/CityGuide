package com.city.guide.controller;

import com.city.guide.dto.Result;
import com.city.guide.service.ICheckInService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 用户打卡相关接口
 */
@RestController
@RequestMapping("/check-in")
public class CheckInController {

    @Resource
    private ICheckInService checkInService;

    /**
     * 用户打卡
     */
    @PostMapping("/do")
    public Result checkIn() {
        return checkInService.checkIn();
    }

    /**
     * 查询指定月份的打卡记录
     */
    @GetMapping("/month")
    public Result getMonthCheckIn(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return checkInService.getMonthCheckIn(year, month);
    }

    /**
     * 查询打卡统计信息
     */
    @GetMapping("/stats")
    public Result getCheckInStats() {
        return checkInService.getCheckInStats();
    }
}
