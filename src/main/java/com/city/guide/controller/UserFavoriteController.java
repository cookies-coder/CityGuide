package com.city.guide.controller;

import com.city.guide.dto.FavoriteRequestDTO;
import com.city.guide.dto.Result;
import com.city.guide.service.IUserFavoriteService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 用户收藏相关接口
 */
@RestController
@RequestMapping("/favorite") // 基础路径
public class UserFavoriteController {

    @Resource
    private IUserFavoriteService favoriteService;

    /**
     * 添加收藏
     */
    @PostMapping("/add")
    public Result addFavorite(@RequestBody FavoriteRequestDTO favoriteDTO) {
        return favoriteService.addFavorite(favoriteDTO);
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/delete/{spotId}")
    public Result deleteFavorite(@PathVariable Long spotId) {
        return favoriteService.deleteFavorite(spotId);
    }

    /**
     * 判断当前用户是否收藏了该景点
     */
    @GetMapping("/isFavorite/{spotId}")
    public Result isFavorite(@PathVariable Long spotId) {
        return favoriteService.isFavorite(spotId);
    }

    /**
     * 分页查询我的收藏夹
     * 默认查第1页，每页10条
     */
    @GetMapping("/list")
    public Result queryMyFavorites(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return favoriteService.queryMyFavorites(page, size);
    }
}