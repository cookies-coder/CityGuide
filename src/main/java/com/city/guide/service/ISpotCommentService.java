package com.city.guide.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.city.guide.entity.SpotComment;

import java.util.List;

/**
 * <p>
 * 景点评论服务类
 * </p>
 *
 * @Cookie-coder
 * 
 */
public interface ISpotCommentService extends IService<SpotComment> {

    /**
     * 发布景点评论
     * @param spotComment 评论信息
     * @return 是否成功
     */
    boolean saveSpotComment(SpotComment spotComment);

    /**
     * 获取指定景点的最新评论（最多5条）
     * @param spotId 景点ID
     * @return 评论列表
     */
    List<SpotComment> getLatestCommentsBySpotId(Long spotId);
}