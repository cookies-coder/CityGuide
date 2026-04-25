package com.city.guide.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.city.guide.dto.Result;
import com.city.guide.entity.CheckIn;
import com.city.guide.mapper.CheckInMapper;
import com.city.guide.service.ICheckInService;
import com.city.guide.utils.RedisIdWorker;
import com.city.guide.utils.TravelerContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户打卡业务实现类
 */
@Service
public class CheckInServiceImpl extends ServiceImpl<CheckInMapper, CheckIn> implements ICheckInService {

    @Resource
    private RedisIdWorker redisIdWorker;

    @Override
    public Result checkIn() {
        // 1. 获取当前用户
        Long userId = TravelerContext.getTraveler().getId();
        
        // 2. 获取当前日期
        LocalDate today = LocalDate.now();
        
        // 3. 检查今天是否已经打卡
        Long count = lambdaQuery()
                .eq(CheckIn::getUserId, userId)
                .eq(CheckIn::getDate, today)
                .count();
        
        if (count > 0) {
            return Result.fail("今天已经打卡过了");
        }
        
        // 4. 创建打卡记录
        CheckIn checkIn = new CheckIn();
        checkIn.setId(redisIdWorker.nextId("check_in"));
        checkIn.setUserId(userId);
        checkIn.setYear(today.getYear());
        checkIn.setMonth(today.getMonthValue());
        checkIn.setDate(today);
        checkIn.setIsBackup(0); // 0表示正常打卡
        
        // 5. 保存到数据库
        boolean success = save(checkIn);
        if (!success) {
            return Result.fail("打卡失败，请稍后重试");
        }
        
        return Result.ok("打卡成功");
    }

    @Override
    public Result getMonthCheckIn(Integer year, Integer month) {
        // 1. 获取当前用户
        Long userId = TravelerContext.getTraveler().getId();
        
        // 2. 查询指定年月的打卡记录
        List<CheckIn> checkInList = lambdaQuery()
                .eq(CheckIn::getUserId, userId)
                .eq(CheckIn::getYear, year)
                .eq(CheckIn::getMonth, month)
                .orderByAsc(CheckIn::getDate)
                .list();
        
        return Result.ok(checkInList);
    }

    @Override
    public Result getCheckInStats() {
        // 1. 获取当前用户
        Long userId = TravelerContext.getTraveler().getId();
        
        // 2. 统计总打卡次数
        Long totalCount = lambdaQuery()
                .eq(CheckIn::getUserId, userId)
                .count();
        
        // 3. 统计本月打卡次数
        LocalDate today = LocalDate.now();
        Long monthCount = lambdaQuery()
                .eq(CheckIn::getUserId, userId)
                .eq(CheckIn::getYear, today.getYear())
                .eq(CheckIn::getMonth, today.getMonthValue())
                .count();
        
        // 4. 查询连续打卡天数（从昨天往前推）
        Integer continuousDays = calculateContinuousDays(userId);
        
        // 5. 返回统计信息
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", totalCount);
        stats.put("monthCount", monthCount);
        stats.put("continuousDays", continuousDays);
        
        return Result.ok(stats);
    }

    /**
     * 计算连续打卡天数
     */
    private Integer calculateContinuousDays(Long userId) {
        LocalDate today = LocalDate.now();
        Integer continuousDays = 0;
        
        // 从今天开始往前查询，直到某天没有打卡记录为止
        for (int i = 0; i <= 365; i++) {
            LocalDate checkDate = today.minusDays(i);
            
            Long count = lambdaQuery()
                    .eq(CheckIn::getUserId, userId)
                    .eq(CheckIn::getDate, checkDate)
                    .count();
            
            if (count > 0) {
                continuousDays++;
            } else {
                break;
            }
        }
        
        return continuousDays;
    }
}
