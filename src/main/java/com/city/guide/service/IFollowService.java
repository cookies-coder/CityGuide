package com.city.guide.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.city.guide.dto.Result;
import com.city.guide.entity.Follow;

/**
 * <p>
 * 关注服务接口
 * </p>
 *
 * @Cookie-coder
 * 
 */
public interface IFollowService extends IService<Follow> {

    /**
     * 关注/取关用户（切换逻辑）
     */
    Result followOrUnfollow(Long followUserId);

    /**
     * 判断当前用户是否关注了指定用户
     */
    Result isFollow(Long userId);

    /**
     * 分页查询我的关注列表
     */
    Result queryFollowList(Integer current, Integer size);

    /**
     * 分页查询我的粉丝列表
     */
    Result queryFansList(Integer current, Integer size);

    /**
     * 查询与指定用户的共同关注
     */
    Result queryCommonFollow(Long userId);

}
