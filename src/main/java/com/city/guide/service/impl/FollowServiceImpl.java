package com.city.guide.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.city.guide.dto.Result;
import com.city.guide.dto.UserDTO;
import com.city.guide.entity.Follow;
import com.city.guide.entity.User;
import com.city.guide.mapper.FollowMapper;
import com.city.guide.mapper.UserMapper;
import com.city.guide.service.IFollowService;
import com.city.guide.utils.RedisConstants;
import com.city.guide.utils.TravelerContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 关注服务实现类
 *
 * @Cookie-coder
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserMapper userMapper;

    // ==================== 关注 / 取关 ====================

    @Override
    public Result followOrUnfollow(Long followUserId) {
        // 1. 获取当前登录用户
        UserDTO me = TravelerContext.getTraveler();
        Long userId = me.getId();

        // 2. 不能关注自己
        if (userId.equals(followUserId)) {
            return Result.fail("不能关注自己");
        }

        // 3. 查询是否已经关注
        Follow follow = lambdaQuery()
                .eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, followUserId)
                .one();

        if (follow != null) {
            // 已关注 → 取关：删除 DB 记录 + 移除 Redis Set
            removeById(follow.getId());
            stringRedisTemplate.opsForSet().remove(RedisConstants.FOLLOW_KEY + userId, followUserId.toString());
        } else {
            // 未关注 → 关注：插入 DB 记录 + 加入 Redis Set
            Follow newFollow = new Follow();
            newFollow.setUserId(userId);
            newFollow.setFollowUserId(followUserId);
            save(newFollow);
            stringRedisTemplate.opsForSet().add(RedisConstants.FOLLOW_KEY + userId, followUserId.toString());
        }

        return Result.ok();
    }

    // ==================== 是否关注 ====================

    @Override
    public Result isFollow(Long userId) {
        // 1. 获取当前登录用户
        Long meId = TravelerContext.getTraveler().getId();

        // 2. 优先查 Redis Set
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(RedisConstants.FOLLOW_KEY + meId, userId.toString());
        if (isMember != null && isMember) {
            return Result.ok(true);
        }

        // 3. Redis 未命中，查 DB 并回填缓存
        Follow follow = lambdaQuery()
                .eq(Follow::getUserId, meId)
                .eq(Follow::getFollowUserId, userId)
                .one();

        if (follow != null) {
            // 回填 Redis
            stringRedisTemplate.opsForSet().add(RedisConstants.FOLLOW_KEY + meId, userId.toString());
            return Result.ok(true);
        }

        return Result.ok(false);
    }

    // ==================== 我的关注列表（分页） ====================

    @Override
    public Result queryFollowList(Integer current, Integer size) {
        Long userId = TravelerContext.getTraveler().getId();

        Page<Follow> page = new Page<>(current, size);
        Page<Follow> result = baseMapper.queryFollowList(page, userId);

        return Result.ok(result.getRecords(), result.getTotal());
    }

    // ==================== 我的粉丝列表（分页） ====================

    @Override
    public Result queryFansList(Integer current, Integer size) {
        Long userId = TravelerContext.getTraveler().getId();

        Page<Follow> page = new Page<>(current, size);
        Page<Follow> result = baseMapper.queryFansList(page, userId);

        return Result.ok(result.getRecords(), result.getTotal());
    }

    // ==================== 共同关注 ====================

    @Override
    public Result queryCommonFollow(Long userId) {
        Long meId = TravelerContext.getTraveler().getId();

        // 1. 优先用 Redis Set 交集运算
        Set<String> commonIds = stringRedisTemplate.opsForSet()
                .intersect(RedisConstants.FOLLOW_KEY + meId, RedisConstants.FOLLOW_KEY + userId);

        List<Long> idList;

        if (commonIds != null && !commonIds.isEmpty()) {
            // Redis 命中
            idList = commonIds.stream()
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
        } else {
            // 2. Redis 未命中（冷启动），用 DB 兜底查询
            idList = baseMapper.queryCommonFollowIds(meId, userId);

            // 3. 回填 Redis Set，避免下次还走 DB
            if (!idList.isEmpty()) {
                initFollowSet(meId);
                initFollowSet(userId);
            }
        }

        if (idList.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        // 4. 批量查询用户信息
        List<User> users = userMapper.selectBatchIds(idList);

        // 5. 转为 UserDTO 返回（脱敏，不暴露密码等信息）
        List<UserDTO> dtoList = users.stream().map(u -> {
            UserDTO dto = new UserDTO();
            dto.setId(u.getId());
            dto.setNickName(u.getNickName());
            dto.setIcon(u.getIcon());
            return dto;
        }).collect(Collectors.toList());

        return Result.ok(dtoList);
    }

    /**
     * 初始化用户的关注 Redis Set（冷启动时调用）
     */
    private void initFollowSet(Long userId) {
        String key = RedisConstants.FOLLOW_KEY + userId;
        Boolean exists = stringRedisTemplate.hasKey(key);
        if (exists != null && exists) {
            return;
        }
        // 从 DB 加载全部关注 ID 到 Redis Set
        List<Follow> follows = lambdaQuery()
                .eq(Follow::getUserId, userId)
                .list();
        if (!follows.isEmpty()) {
            String[] ids = follows.stream()
                    .map(f -> f.getFollowUserId().toString())
                    .toArray(String[]::new);
            stringRedisTemplate.opsForSet().add(key, ids);
        }
    }
}
