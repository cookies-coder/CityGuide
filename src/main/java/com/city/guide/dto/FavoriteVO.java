package com.city.guide.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 
 */
@Data
public class FavoriteVO {
    // 景点ID
    private Long spotId;

    // 景点名称
    private String name;

    // 景点类型
    private String type;

    // 景点主图
    private String image;

    // 景点地址
    private String address;

    // 收藏时写的备注
    private String notes;

    // 收藏时间
    private LocalDateTime createTime;
}