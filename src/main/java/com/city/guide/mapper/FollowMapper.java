package com.city.guide.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.city.guide.entity.Follow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 关注 Mapper 接口
 * </p>
 *
 * @Cookie-coder
 * 
 */
public interface FollowMapper extends BaseMapper<Follow> {

    /**
     * 分页查询我的关注列表
     * 联查被关注用户的昵称和头像
     */
    @Select("SELECT f.*, u.nick_name AS follow_user_nick_name, u.icon AS follow_user_icon " +
            "FROM cg_follow f " +
            "LEFT JOIN cg_user u ON f.follow_user_id = u.id " +
            "WHERE f.user_id = #{userId} " +
            "ORDER BY f.create_time DESC")
    Page<Follow> queryFollowList(Page<Follow> page, @Param("userId") Long userId);

    /**
     * 分页查询我的粉丝列表
     * 联查粉丝的昵称和头像
     */
    @Select("SELECT f.*, u.nick_name AS follow_user_nick_name, u.icon AS follow_user_icon " +
            "FROM cg_follow f " +
            "LEFT JOIN cg_user u ON f.user_id = u.id " +
            "WHERE f.follow_user_id = #{userId} " +
            "ORDER BY f.create_time DESC")
    Page<Follow> queryFansList(Page<Follow> page, @Param("userId") Long userId);

    /**
     * 查询两个用户的共同关注 ID 列表
     */
    @Select("SELECT follow_user_id FROM cg_follow WHERE user_id = #{userId1} " +
            "AND follow_user_id IN (SELECT follow_user_id FROM cg_follow WHERE user_id = #{userId2})")
    List<Long> queryCommonFollowIds(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

}

