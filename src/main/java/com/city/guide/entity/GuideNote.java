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
 * 旅行笔记实体类
 * </p>
 *
 * @Cookie-coder
 * 
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("cg_guide_note")
public class GuideNote implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 景点id
     */
    private Long spotId;
    /**
     * 旅行者id
     */
    private Long userId;
    /**
     * 旅行者图标
     */
    @TableField(exist = false)
    private String icon;
    /**
     * 是否点过赞
     */
    @TableField(exist = false)
    private Boolean isLike;
    /**
     * 旅行者姓名
     */
    @TableField(exist = false)
    private String name;

    /**
     * 标题
     */
    private String title;

    /**
     * 旅行照片，最多9张，多张以","隔开
     */
    private String images;

    /**
     * 旅行的文字描述
     */
    private String content;

    /**
     * 点赞数量
     */
    private Integer liked;

    /**
     * 评论数量
     */
    private Integer comments;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
