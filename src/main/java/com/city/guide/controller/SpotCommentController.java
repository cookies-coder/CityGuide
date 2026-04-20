package com.city.guide.controller;

import com.city.guide.dto.Result;
import com.city.guide.dto.UserDTO;
import com.city.guide.entity.SpotComment;
import com.city.guide.service.ISpotCommentService;
import com.city.guide.utils.TravelerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 景点评论前端控制器
 * </p>
 *
 * @Cookie-coder
 * 
 */
@Slf4j
@RestController
@RequestMapping("/comment")
public class SpotCommentController {

    @Resource
    private ISpotCommentService spotCommentService;

    /**
     * 发布景点评论
     * @param spotComment 评论信息（包含spotId、content、tag）
     * @return 操作结果
     */
    @PostMapping("/save")
    public Result saveComment(@RequestBody SpotComment spotComment) {
        // 获取当前登录用户
        UserDTO userDTO = TravelerContext.getTraveler();
        if (userDTO == null) {
            return Result.fail("请先登录");
        }

        // 参数校验
        if (spotComment.getSpotId() == null) {
            return Result.fail("景点ID不能为空");
        }
        if (spotComment.getContent() == null || spotComment.getContent().trim().isEmpty()) {
            return Result.fail("评论内容不能为空");
        }

        // 设置用户ID和创建时间
        spotComment.setUserId(userDTO.getId());
        spotComment.setCreateTime(LocalDateTime.now());
        spotComment.setUpdateTime(LocalDateTime.now());
        spotComment.setLikeCount(0);

        // 保存评论
        boolean success = spotCommentService.saveSpotComment(spotComment);
        
        if (success) {
            return Result.ok("评论发布成功");
        }
        return Result.fail("评论发布失败");
    }

    /**
     * 获取指定景点的最新评论（最多5条）
     * @param spotId 景点ID
     * @return 评论列表
     */
    @GetMapping("/list/{spotId}")
    public Result getLatestComments(@PathVariable Long spotId) {
        if (spotId == null) {
            return Result.fail("景点ID不能为空");
        }

        List<SpotComment> comments = spotCommentService.getLatestCommentsBySpotId(spotId);
        return Result.ok(comments);
    }
}