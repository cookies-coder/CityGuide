package com.city.guide.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.city.guide.dto.Result;
import com.city.guide.entity.GuideNote;

import java.util.List;

/**
 * <p>
 * 旅行笔记服务类
 * </p>
 *
 * @Cookie-coder
 * 
 */
public interface IGuideNoteService extends IService<GuideNote> {
    Result queryHotGuideNote(int current);
    Long saveGuideNote(GuideNote guideNote);
    List<GuideNote> queryGuideNote(int current);

    Result queryGuideNoteById(Long id);

    Result likeNote(Long id);
}
