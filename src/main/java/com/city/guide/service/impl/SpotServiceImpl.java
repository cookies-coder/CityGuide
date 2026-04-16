package com.city.guide.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.city.guide.dto.Result;
import com.city.guide.entity.Spot;
import com.city.guide.mapper.SpotMapper;
import com.city.guide.service.ISpotService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static com.city.guide.utils.RedisConstants.CACHE_SPOT_KEY;

/**
 * <p>
 * 景点服务实现类
 * </p>
 *
 * @Cookie-coder
 * 
 */
@Service
public class SpotServiceImpl extends ServiceImpl<SpotMapper, Spot> implements ISpotService {


    /**
     * 根据ID查询景点信息（带Redis缓存优化）
     * 业务逻辑：优先命中缓存，若缓存失效则查询数据库并回写缓存
     * * @param id 景点唯一标识
     * @return 包含景点数据的Result对象
     */
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        // 1. 尝试从 Redis 中获取景点缓存数据
        //  Key 设计遵循：业务名前缀 + 景点ID，保证全局唯一性
        String spotJson = stringRedisTemplate.opsForValue().get(CACHE_SPOT_KEY + id);

        // 2. 缓存命中处理：判断缓存是否有效
        if (StrUtil.isNotBlank(spotJson)) {
            // 缓存命中直接反序列化并返回，极大地减轻了 MySQL 数据库的查询压力
            Spot spot = JSONUtil.toBean(spotJson, Spot.class);
            return Result.ok(spot);
        }

        // 3. 缓存未命中：通过主键去数据库查询
        Spot spot = getById(id);

        // 4. 数据库边界检查：处理空数据情况
        if (spot == null) {
            // 如果数据库也没有该景点，直接拦截并返回错误信息
            return Result.fail("你要查询的景点不存在");
        }

        // 5. 缓存重建：将数据库查询到的真实数据写入 Redis
        // 这里采用简单的 SET 操作
        stringRedisTemplate.opsForValue().set(CACHE_SPOT_KEY + id, JSONUtil.toJsonStr(spot));

        // 6. 最终返回数据
        return Result.ok(spot);
    }
}
