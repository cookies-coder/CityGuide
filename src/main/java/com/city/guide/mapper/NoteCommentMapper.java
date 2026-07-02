package com.city.guide.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.city.guide.entity.NoteComment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 笔记评论 Mapper 接口
 * </p>
 *
 * @Cookie-coder
 * 
 */
public interface NoteCommentMapper extends BaseMapper<NoteComment> {

    /**
     * 分页查询指定笔记的一级评论（parentId = 0）
     * 联查评论者昵称和头像
     * 按时间倒序（最新评论在前）
     */
    @Select("SELECT c.*, u.nick_name AS user_name, u.icon AS user_icon " +
            "FROM cg_note_comment c " +
            "LEFT JOIN cg_user u ON c.user_id = u.id " +
            "WHERE c.note_id = #{noteId} AND c.parent_id = 0 " +
            "ORDER BY c.create_time DESC")
    Page<NoteComment> queryTopComments(Page<NoteComment> page, @Param("noteId") Long noteId);

    /**
     * 分页查询指定一级评论下的子评论
     * 联查评论者昵称/头像 + 被回复人昵称
     * 按时间正序（早期评论在前）
     */
    @Select("SELECT c.*, u.nick_name AS user_name, u.icon AS user_icon, " +
            "a.nick_name AS answer_name " +
            "FROM cg_note_comment c " +
            "LEFT JOIN cg_user u ON c.user_id = u.id " +
            "LEFT JOIN cg_user a ON c.answer_id = a.id " +
            "WHERE c.parent_id = #{parentId} " +
            "ORDER BY c.create_time ASC")
    Page<NoteComment> queryReplies(Page<NoteComment> page, @Param("parentId") Long parentId);

    /**
     * 统计指定一级评论下的子评论总数（删除时用于更新笔记 comments 字段）
     */
    @Select("SELECT COUNT(*) FROM cg_note_comment WHERE parent_id = #{parentId}")
    Integer countRepliesByParentId(@Param("parentId") Long parentId);

}
