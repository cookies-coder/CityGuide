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
 * 
 * </p>
 *
 * @Cookie-coder
 * 
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("cg_follow")
public class Follow implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 关联的用户id
     */
    private Long followUserId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 被关注用户昵称（非数据库字段，联查填充）
     */
    @TableField(exist = false)
    private String followUserNickName;

    /**
     * 被关注用户头像（非数据库字段，联查填充）
     */
    @TableField(exist = false)
    private String followUserIcon;

}
