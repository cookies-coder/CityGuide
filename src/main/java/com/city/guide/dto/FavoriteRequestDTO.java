package com.city.guide.dto;

import lombok.Data;

/**
 *
 */
@Data
public class FavoriteRequestDTO {
    // 景点ID
    private Long spotId;

    // 用户对景点的备注
    private String notes;
}