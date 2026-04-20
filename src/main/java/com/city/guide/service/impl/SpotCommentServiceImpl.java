package com.city.guide.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.city.guide.entity.SpotComment;
import com.city.guide.mapper.SpotCommentMapper;
import com.city.guide.service.ISpotCommentService;
import com.city.guide.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 景点评论服务实现类
 * </p>
 *
 * @Cookie-coder
 * 
 */
@Service
public class SpotCommentServiceImpl extends ServiceImpl<SpotCommentMapper, SpotComment> implements ISpotCommentService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final Long SPOT_COMMENT_CACHE_TTL = RedisConstants.SPOT_COMMENT_CACHE_TTL;

    @Override
    public boolean saveSpotComment(SpotComment spotComment) {
        // 保存到数据库
        boolean success = save(spotComment);
        
        if (success) {
            // 清除该景点的评论缓存，确保下次查询能获取到最新评论
            String cacheKey = RedisConstants.SPOT_COMMENT_CACHE_KEY + spotComment.getSpotId();
            stringRedisTemplate.delete(cacheKey);
        }
        
        return success;
    }

    @Override
    public List<SpotComment> getLatestCommentsBySpotId(Long spotId) {
        // 先从Redis缓存中查询
        String cacheKey = RedisConstants.SPOT_COMMENT_CACHE_KEY + spotId;
        String cacheData = stringRedisTemplate.opsForValue().get(cacheKey);
        
        if (cacheData != null) {
            // 缓存命中，直接返回
            return JSONUtil.toList(cacheData, SpotComment.class);
        }
        
        // 缓存未命中，从数据库查询
        LambdaQueryWrapper<SpotComment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SpotComment::getSpotId, spotId)
                   .orderByDesc(SpotComment::getCreateTime)
                   .last("LIMIT 5");
        
        List<SpotComment> comments = list(queryWrapper);
        
        // 将查询结果写入缓存
        if (!comments.isEmpty()) {
            stringRedisTemplate.opsForValue().set(
                cacheKey, 
                JSONUtil.toJsonStr(comments), 
                SPOT_COMMENT_CACHE_TTL, 
                TimeUnit.MINUTES
            );
        }
        
        return comments;
    }
}