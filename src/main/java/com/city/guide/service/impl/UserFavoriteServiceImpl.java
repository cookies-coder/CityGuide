package com.city.guide.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.city.guide.dto.FavoriteRequestDTO;
import com.city.guide.dto.FavoriteVO;
import com.city.guide.dto.Result;
import com.city.guide.entity.UserFavorite;
import com.city.guide.mapper.UserFavoriteMapper;
import com.city.guide.service.IUserFavoriteService;
import com.city.guide.utils.TravelerContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import static com.city.guide.utils.RedisConstants.USE_FAVORITE_KEY;
@Service
public class UserFavoriteServiceImpl extends ServiceImpl<UserFavoriteMapper, UserFavorite> implements IUserFavoriteService {
   @Resource
   private StringRedisTemplate stringRedisTemplate;
    @Override
    @Transactional
    public Result addFavorite(FavoriteRequestDTO favoriteDTO) {
        // 获取用户id
        Long userId = TravelerContext.getTraveler().getId();
        // 获取景点id
        Long spotId = favoriteDTO.getSpotId();
        Integer counts = lambdaQuery().eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getSpotId, spotId).count();
        if (counts > 0) {
            return Result.fail("您已经收藏过该景点");
        }
        // 用户未收藏则添加收藏数据到数据库
        UserFavorite userFavorite = new UserFavorite();
        userFavorite.setUserId(userId);
        userFavorite.setSpotId(spotId);
        userFavorite.setNotes(favoriteDTO.getNotes());
        boolean result = save(userFavorite);
        // 如果收藏失败则返回异常
        if (!result) {
            return Result.fail("抱歉，服务器出现错误，收藏失败");
        }
        // 将收藏数据添加到Redis缓存
        stringRedisTemplate.opsForSet().add(USE_FAVORITE_KEY + userId, spotId.toString());
        return Result.ok("收藏成功");
    }

    @Override
    @Transactional
    public Result deleteFavorite(Long spotId) {
        // 获取用户id
        Long userId = TravelerContext.getTraveler().getId();
        // 执行取消收藏操作
        boolean result = lambdaUpdate().eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getSpotId, spotId).remove();
        // 如果取消收藏失败则返回异常
        if (!result) {
            return Result.fail("抱歉，服务器出现错误，取消收藏失败");
        }
        // 将取消收藏数据从Redis缓存中移除
        stringRedisTemplate.opsForSet().remove(USE_FAVORITE_KEY + userId, spotId.toString());
        return Result.ok("取消收藏成功");
    }

    @Override
    public Result queryMyFavorites(Integer page, Integer size) {
        // 1. 获取当前登录人的ID
        Long userId = TravelerContext.getTraveler().getId();

        // 2. 创建一个分页助手对象
        Page<FavoriteVO> pageParams = new Page<>(page, size);

        // 3. 调用 Mapper 里的自定义查询方法
        // 把分页助手和用户ID传进去
        Page<FavoriteVO> resultPage = baseMapper.queryMyFavorites(pageParams, userId);

        // 4. 把查出来的结果包含总条数、当前页数据等返回给前端
        return Result.ok(resultPage);
    }

    @Override
    public Result isFavorite(Long spotId) {
        Long userId = TravelerContext.getTraveler().getId();
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(USE_FAVORITE_KEY + userId, spotId.toString());
        return Result.ok(Boolean.TRUE.equals(isMember));
    }
}
