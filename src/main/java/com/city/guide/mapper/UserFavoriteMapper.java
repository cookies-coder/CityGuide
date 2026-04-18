package com.city.guide.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.city.guide.dto.FavoriteVO;
import com.city.guide.entity.UserFavorite;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户收藏 Mapper 接口
 */
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {

    /**
     * 分页查询我的收藏列表（关联 cg_spot 表获取景点详细信息）
     * 这里写一个简单的关联查询 SQL
     */
    @Select("SELECT f.spot_id, s.name, s.type, s.image, s.address, f.notes, f.create_time " +
            "FROM cg_user_favorite f " +
            "LEFT JOIN cg_spot s ON f.spot_id = s.id " +
            "WHERE f.user_id = #{userId} " +
            "ORDER BY f.create_time DESC")
    Page<FavoriteVO> queryMyFavorites(Page<FavoriteVO> page, @Param("userId") Long userId);

}