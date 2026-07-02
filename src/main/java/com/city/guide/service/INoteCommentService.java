package com.city.guide.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.city.guide.dto.Result;
import com.city.guide.entity.NoteComment;

/**
 * <p>
 * 笔记评论服务类
 * </p>
 *
 * @Cookie-coder
 * 
 */
public interface INoteCommentService extends IService<NoteComment> {

    /**
     * 发布评论（一级评论或子评论）
     */
    Result saveNoteComment(NoteComment noteComment);

    /**
     * 删除评论（只允许本人，一级评论连带删子评论）
     */
    Result deleteNoteComment(Long commentId);

    /**
     * 分页查询指定笔记的一级评论
     */
    Result queryTopComments(Long noteId, Integer current, Integer size);

    /**
     * 分页查询指定一级评论下的子评论
     */
    Result queryReplies(Long parentId, Integer current, Integer size);

    /**
     * 点赞/取消点赞评论（切换逻辑）
     */
    Result likeComment(Long commentId);

}
