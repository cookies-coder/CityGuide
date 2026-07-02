package com.city.guide.controller;

import com.city.guide.dto.Result;
import com.city.guide.entity.NoteComment;
import com.city.guide.service.INoteCommentService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 笔记评论前端控制器
 * </p>
 *
 * @Cookie-coder
 * 
 */
@RestController
@RequestMapping("/note-comment")
public class NoteCommentController {

    @Resource
    private INoteCommentService noteCommentService;

    /**
     * 发布评论（一级评论或子评论）
     * parentId = 0 表示一级评论，parentId > 0 表示回复某条一级评论
     * answerId 表示子评论中具体回复了哪条评论
     *
     * @param noteComment 评论信息（noteId、parentId、answerId、content）
     * @return 评论id
     */
    @PostMapping
    public Result saveNoteComment(@RequestBody NoteComment noteComment) {
        return noteCommentService.saveNoteComment(noteComment);
    }

    /**
     * 删除评论（只允许本人，一级评论连带删除子评论）
     *
     * @param id 评论id
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result deleteNoteComment(@PathVariable("id") Long id) {
        return noteCommentService.deleteNoteComment(id);
    }

    /**
     * 分页查询指定笔记的一级评论（按时间倒序，最新在前）
     *
     * @param noteId  笔记id
     * @param current 当前页码，默认1
     * @param size    每页条数，默认10
     * @return 分页评论列表（含评论者昵称、头像）
     */
    @GetMapping("/top/{noteId}")
    public Result queryTopComments(
            @PathVariable("noteId") Long noteId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return noteCommentService.queryTopComments(noteId, current, size);
    }

    /**
     * 分页查询指定一级评论下的子评论（按时间正序，早期在前）
     *
     * @param parentId 一级评论id
     * @param current  当前页码，默认1
     * @param size     每页条数，默认10
     * @return 分页子评论列表（含评论者昵称、头像、被回复人昵称）
     */
    @GetMapping("/replies/{parentId}")
    public Result queryReplies(
            @PathVariable("parentId") Long parentId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        return noteCommentService.queryReplies(parentId, current, size);
    }

    /**
     * 点赞/取消点赞评论（切换逻辑）
     *
     * @param id 评论id
     * @return 操作结果
     */
    @PutMapping("/like/{id}")
    public Result likeComment(@PathVariable("id") Long id) {
        return noteCommentService.likeComment(id);
    }

}
