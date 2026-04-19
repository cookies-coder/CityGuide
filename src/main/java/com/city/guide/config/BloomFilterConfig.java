package com.city.guide.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BloomFilterConfig {

    @Bean
    public RBloomFilter<Long> spotBloomFilter(RedissonClient redissonClient) {
        // 获取布隆过滤器实例
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("spot-id-filter");

        // 初始化：预期插入10万个数据，误差率 0.01
        bloomFilter.tryInit(100000L, 0.01);

        return bloomFilter;
    }
}