package com.city.guide.config;

import com.city.guide.entity.Spot;
import com.city.guide.mapper.SpotMapper;
import org.redisson.api.RBloomFilter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 布隆过滤器初始化器
 * 项目启动时将数据库中已有的景点ID加载到布隆过滤器
 */
@Component
public class BloomFilterInitializer implements CommandLineRunner {

    @Resource
    private RBloomFilter<Long> spotBloomFilter;

    @Resource
    private SpotMapper spotMapper;

    @Override
    public void run(String... args) {
        // 查询数据库中所有景点ID
        List<Spot> spots = spotMapper.selectList(null);
        
        // 将ID批量添加到布隆过滤器
        for (Spot spot : spots) {
            if (spot.getId() != null) {
                spotBloomFilter.add(spot.getId());
            }
        }
        
        System.out.println("布隆过滤器初始化完成，当前有 " + spots.size() + " 个景点");
    }
}
