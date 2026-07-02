package com.city.guide.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.city.guide.dto.Result;
import com.city.guide.entity.Spot;
import com.city.guide.mapper.SpotMapper;
import com.city.guide.service.impl.SpotServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBloomFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import static com.city.guide.utils.RedisConstants.CACHE_SPOT_KEY;
import static com.city.guide.utils.RedisConstants.CACHE_SPOT_TTL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SpotMapper spotMapper;

    @Mock
    private RBloomFilter<Long> spotBloomFilter;

    @InjectMocks
    private SpotServiceImpl spotService;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        // 反射设置 MyBatis Plus 的 baseMapper
        Field field = ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(spotService, spotMapper);
    }

    @Test
    void testQueryById_BloomFilterNotContains() {
        when(spotBloomFilter.contains(999L)).thenReturn(false);

        Result result = spotService.queryById(999L);

        assertFalse(result.getSuccess());
        assertEquals("你要查询的景点不存在", result.getErrorMsg());
        // 布隆过滤器拦截后不应该访问 Redis
        verify(valueOperations, never()).get(anyString());
    }

    @Test
    void testQueryById_CacheHit() {
        Spot spot = new Spot().setId(1L).setName("东方明珠").setX(121.5).setY(31.2).setTypeId(1L);
        when(spotBloomFilter.contains(1L)).thenReturn(true);
        when(valueOperations.get(CACHE_SPOT_KEY + 1L)).thenReturn(JSONUtil.toJsonStr(spot));

        Result result = spotService.queryById(1L);

        assertTrue(result.getSuccess());
        assertNotNull(result.getData());
        Spot resultSpot = (Spot) result.getData();
        assertEquals("东方明珠", resultSpot.getName());
        // 坐标转换后应该设置百度坐标
        assertNotNull(resultSpot.getBdX());
        assertNotNull(resultSpot.getBdY());
        // 缓存命中不应查数据库
        verify(spotMapper, never()).selectById(any());
    }

    @Test
    void testQueryById_CacheMiss_DbHit() {
        Spot spot = new Spot().setId(2L).setName("外滩").setX(121.49).setY(31.24).setTypeId(1L);
        when(spotBloomFilter.contains(2L)).thenReturn(true);
        when(valueOperations.get(CACHE_SPOT_KEY + 2L)).thenReturn(null);
        when(spotMapper.selectById(2L)).thenReturn(spot);

        Result result = spotService.queryById(2L);

        assertTrue(result.getSuccess());
        assertNotNull(result.getData());
        Spot resultSpot = (Spot) result.getData();
        assertEquals("外滩", resultSpot.getName());
        assertNotNull(resultSpot.getBdX());
        assertNotNull(resultSpot.getBdY());
        // 验证数据写入了缓存
        verify(valueOperations, times(1))
                .set(eq(CACHE_SPOT_KEY + 2L), anyString(), eq(CACHE_SPOT_TTL), eq(TimeUnit.MINUTES));
    }

    @Test
    void testQueryById_CacheMiss_DbMiss() {
        when(spotBloomFilter.contains(888L)).thenReturn(true);
        when(valueOperations.get(CACHE_SPOT_KEY + 888L)).thenReturn(null);
        when(spotMapper.selectById(888L)).thenReturn(null);

        Result result = spotService.queryById(888L);

        assertFalse(result.getSuccess());
        assertEquals("你要查询的景点不存在", result.getErrorMsg());
        // 数据库没有数据不应写缓存
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void testSave_ShouldAddToBloomFilter() {
        Spot spot = new Spot().setName("豫园").setX(121.49).setY(31.23).setTypeId(2L);
        when(spotMapper.insert(spot)).thenAnswer(invocation -> {
            spot.setId(10L);
            return 1;
        });
        when(spotBloomFilter.add(10L)).thenReturn(true);

        boolean result = spotService.save(spot);

        assertTrue(result);
        verify(spotBloomFilter, times(1)).add(10L);
    }

    @Test
    void testUpdate_ShouldDeleteCache() {
        Spot spot = new Spot().setId(3L).setName("南京路");
        when(spotMapper.updateById(spot)).thenReturn(1);

        Result result = spotService.updata(spot);

        assertTrue(result.getSuccess());
        verify(spotMapper, times(1)).updateById(spot);
        // 验证删除了 Redis 缓存
        verify(stringRedisTemplate, times(1)).delete(CACHE_SPOT_KEY + 3L);
    }
}
