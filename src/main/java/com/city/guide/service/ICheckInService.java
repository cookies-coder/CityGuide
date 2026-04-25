package com.city.guide.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.city.guide.dto.Result;
import com.city.guide.entity.CheckIn;

/**
 * 用户打卡业务层接口
 */
public interface ICheckInService extends IService<CheckIn> {

    /**
     * 用户打卡
     */
    Result checkIn();

    /**
     * 查询指定月份的打卡记录
     */
    Result getMonthCheckIn(Integer year, Integer month);

    /**
     * 查询打卡统计信息
     */
    Result getCheckInStats();
}
