package com.city.guide.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 城市导览全局唯一ID生成器
 * 基于Redis实现，用于生成订单号、攻略笔记ID、评论ID等业务主键
 * 
 * ID结构：高32位为相对时间戳（秒），低32位为当日序列号
 * 支持趋势递增，适用于数据库索引优化
 */
@Component
public class RedisIdWorker {
    /**
     * 起始时间戳：2022-01-01 00:00:00
     * 使用相对时间戳节省位数，延长ID可用年限
     */
    private static final long BEGIN_TIMESTAMP = 1640995200L;
    /**
     * 序列号占用位数：32位
     * 单日最大支持 2^32 = 42.9亿 个ID
     */
    private static final int COUNT_BITS = 32;

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 生成全局唯一ID
     * 
     * @param businessType
     * @return 64位全局唯一ID（long类型）
     */
    public long nextId(String businessType) {
        // 1. 计算相对时间戳（当前秒数 - 起始秒数）
        LocalDateTime now = LocalDateTime.now();
        long currentSecond = now.toEpochSecond(ZoneOffset.UTC);
        long relativeTimestamp = currentSecond - BEGIN_TIMESTAMP;

        // 2. 生成当日自增序列号
        // 2.1 格式化日期，确保每天序列号从0重新开始
        String dateKey = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 2.2 Redis原子自增
        String redisKey = "cg:id:" + businessType + ":" + dateKey;
        long sequence = stringRedisTemplate.opsForValue().increment(redisKey);

        // 3. 位运算拼接
        return relativeTimestamp << COUNT_BITS | sequence;
    }
}

