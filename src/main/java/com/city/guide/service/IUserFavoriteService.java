package com.city.guide.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.city.guide.dto.FavoriteRequestDTO;
import com.city.guide.dto.Result;
import com.city.guide.entity.UserFavorite;

/**
 * 收藏夹业务层接口
 */
public interface IUserFavoriteService extends IService<UserFavorite> {

    /**
     * 添加收藏
     */
    Result addFavorite(FavoriteRequestDTO favoriteDTO);

    /**
     * 取消收藏
     */
    Result deleteFavorite(Long spotId);

    /**
     * 分页查询当前登录人的收藏列表
     */
    Result queryMyFavorites(Integer page, Integer size);

    /**
     * 检查某个景点是否已经被我收藏了
     */
    Result isFavorite(Long spotId);
}