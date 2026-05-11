package com.city.guide.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.city.guide.dto.Result;
import com.city.guide.dto.UserDTO;
import com.city.guide.entity.GuideNote;
import com.city.guide.entity.User;
import com.city.guide.mapper.GuideNoteMapper;
import com.city.guide.service.IGuideNoteService;
import com.city.guide.service.IUserService;
import com.city.guide.utils.SystemConstants;
import com.city.guide.utils.TravelerContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 旅行笔记服务实现类
 * </p>
 *
 * @Cookie-coder
 * 
 */
@Service
public class GuideNoteServiceImpl extends ServiceImpl<GuideNoteMapper, GuideNote> implements IGuideNoteService {
    @Resource
    private IGuideNoteService guideNoteService;
    @Resource
    private IUserService userService;

    @Override
    public Result queryHotGuideNote(int current) {
        // 根据点赞数查询
        Page<GuideNote> page = guideNoteService.query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<GuideNote> records = page.getRecords();
        // 查询旅行者
        records.forEach(note -> {
            Long userId = note.getUserId();
            User user = userService.getById(userId);
            note.setName(user.getNickName());
            note.setIcon(user.getIcon());
        });
        return Result.ok(records);
    }

    @Override
    public Long saveGuideNote(GuideNote guideNote) {
        // 获取登录旅行者
        UserDTO traveler = TravelerContext.getTraveler();
        guideNote.setUserId(traveler.getId());
        // 保存旅行笔记
        guideNoteService.save(guideNote);
        // 返回id
        return guideNote.getId();
    }

    @Override
    public List<GuideNote> queryGuideNote(int current) {
        // 获取登录旅行者
        UserDTO traveler = TravelerContext.getTraveler();
        // 根据旅行者查询
        Page<GuideNote> page = guideNoteService.query()
                .eq("user_id", traveler.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<GuideNote> records = page.getRecords();
        return records;
    }

    @Override
    public Result queryGuideNoteById(Long id) {
        //查询旅行笔记
        GuideNote guideNote = guideNoteService.getById(id);
        if (guideNote == null) {
            return Result.fail("旅行笔记不存在");
        }
        Long userId = guideNote.getUserId();
        User user = userService.getById(userId);
        guideNote.setName(user.getNickName());
        guideNote.setIcon(user.getIcon());

        return Result.ok(guideNote);
    }

    @Override
    public Result likeNote(Long id) {
        return Result.ok();
    }


}
