package com.city.guide.service.impl;

import cn.hutool.core.util.BooleanUtil;
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
import org.springframework.data.redis.core.StringRedisTemplate;
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
    @Resource
    private StringRedisTemplate stringRedisTemplate;

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
            this.querynoteuser(note);
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
        records.forEach(note -> {
            this.querynoteuser(note);
            this.isNoteLiked(note);
        });
        return records;
    }

    @Override
    public Result queryGuideNoteById(Long id) {
        //查询旅行笔记
        GuideNote guideNote = guideNoteService.getById(id);
        if (guideNote == null) {
            return Result.fail("旅行笔记不存在");
        }
        querynoteuser(guideNote);
        //判断用户是否已点赞
        isNoteLiked(guideNote);

        return Result.ok(guideNote);
    }

    private void querynoteuser(GuideNote guideNote) {
        Long userId = guideNote.getUserId();
        User user = userService.getById(userId);
        guideNote.setName(user.getNickName());
        guideNote.setIcon(user.getIcon());
    }

    private void isNoteLiked(GuideNote guideNote) {
        Long userId = TravelerContext.getTraveler().getId();
        String key = "cg:like:note:" + guideNote.getId();
        Boolean isLiked = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        guideNote.setIsLike(BooleanUtil.isTrue(isLiked));
    }

    @Override
    public Result likeNote(Long id) {
       //获取用户id
        Long userId = TravelerContext.getTraveler().getId();
        // 判断用户是否已点赞
        String key = "cg:like:note:" + id;
        Boolean isLiked = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        if(BooleanUtil.isFalse(isLiked)){
            //点赞数加1且将用户id加入Redis集合
            Boolean isadd=update().setSql("liked = liked + 1").eq("id", id).update();
            if(isadd){
                stringRedisTemplate.opsForSet().add(key, userId.toString());
            }
        }
        else if(BooleanUtil.isTrue(isLiked)){
            //点赞数减1且将用户id从Redis集合中移除
            Boolean isremove=update().setSql("liked = liked - 1").eq("id", id).update();
            if(isremove){
                stringRedisTemplate.opsForSet().remove(key, userId.toString());
            }
        }
        return Result.ok();

    }


}
