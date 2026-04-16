package com.city.guide.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.city.guide.dto.Result;
import com.city.guide.entity.SpotType;
import com.city.guide.mapper.SpotTypeMapper;
import com.city.guide.service.ISpotTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 景点类型服务实现类
 * </p>
 *
 * @Cookie-coder
 * 
 */
@Service
public class SpotTypeServiceImpl extends ServiceImpl<SpotTypeMapper, SpotType> implements ISpotTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        String key = "cache:spotType:list";

        // 1. 从 Redis 查询类型
        String typeListJson = stringRedisTemplate.opsForValue().get(key);

        // 2. 判断是否命中
        if (StrUtil.isNotBlank(typeListJson)) {
            List<SpotType> typeList = JSONUtil.toList(typeListJson, SpotType.class);
            return Result.ok(typeList);
        }

        // 3. 缓存未命中，查询数据库所有类型，并按 sort 字段排序
        List<SpotType> typeList = lambdaQuery().orderByAsc(SpotType::getSort).list();

        // 4. 判断数据库是否为空
        if (CollUtil.isEmpty(typeList)) {
            // 返回空集合，防止前端报错
            return Result.fail("暂无该景点类型");
        }

        // 5. 写入 Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(typeList));

        // 6. 返回结果
        return Result.ok(typeList);
    }
}
