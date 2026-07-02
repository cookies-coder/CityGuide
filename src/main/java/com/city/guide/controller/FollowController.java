package com.city.guide.controller;


import com.city.guide.dto.Result;
import com.city.guide.service.IFollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 关注前端控制器
 * </p>
 *
 * @Cookie-coder
 *
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;

    /**
     * 关注/取关用户（切换）
     */
    @PutMapping("/{id}")
    public Result followOrUnfollow(@PathVariable("id") Long followUserId) {
        return followService.followOrUnfollow(followUserId);
    }

    /**
     * 判断是否关注了指定用户
     */
    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id") Long userId) {
        return followService.isFollow(userId);
    }

    /**
     * 分页查询我的关注列表
     */
    @GetMapping("/list")
    public Result queryFollowList(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return followService.queryFollowList(current, size);
    }

    /**
     * 分页查询我的粉丝列表
     */
    @GetMapping("/fans")
    public Result queryFansList(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return followService.queryFansList(current, size);
    }

    /**
     * 查询与指定用户的共同关注
     */
    @GetMapping("/common/{id}")
    public Result queryCommonFollow(@PathVariable("id") Long userId) {
        return followService.queryCommonFollow(userId);
    }
}

