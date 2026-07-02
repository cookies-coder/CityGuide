package com.city.guide.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 笔记评论实体类
 * </p>
 *
 * @Cookie-coder
 * 
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("cg_note_comment")
public class NoteComment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 旅行者id
     */
    private Long userId;

    /**
     * 笔记id
     */
    private Long noteId;

    /**
     * 关联的1级评论id，如果是一级评论，则值为0
     */
    private Long parentId;

    /**
     * 回复的评论id
     */
    private Long answerId;

    /**
     * 回复的内容
     */
    private String content;

    /**
     * 点赞数
     */
    private Integer liked;

    /**
     * 状态，0：正常，1：被举报，2：禁止查看
     */
    private Boolean status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 评论者昵称（非数据库字段，联查填充）
     */
    @TableField(exist = false)
    private String userName;

    /**
     * 评论者头像（非数据库字段，联查填充）
     */
    @TableField(exist = false)
    private String userIcon;

    /**
     * 被回复人昵称（非数据库字段，联查填充）
     */
    @TableField(exist = false)
    private String answerName;

    /**
     * 当前用户是否点赞过该评论（非数据库字段，Redis 查询填充）
     */
    @TableField(exist = false)
    private Boolean isLike;

}
