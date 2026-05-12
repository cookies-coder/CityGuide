package com.city.guide.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
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
import java.util.Set;
import java.util.stream.Collectors;

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
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryHotGuideNote(int current) {
        // 根据点赞数查询
        Page<GuideNote> page = this.query()
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
        this.save(guideNote);
        // 返回id
        return guideNote.getId();
    }

    @Override
    public List<GuideNote> queryGuideNote(int current) {
        // 获取登录旅行者
        UserDTO traveler = TravelerContext.getTraveler();
        // 根据旅行者查询
        Page<GuideNote> page = this.query()
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
        GuideNote guideNote = this.getById(id);
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
        UserDTO userDTO = TravelerContext.getTraveler();
        if (userDTO == null) {
            // 用户未登录，无法判断是否点赞
            return;
        }
        Long userId = userDTO.getId();
        String key = "cg:like:note:" + guideNote.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        guideNote.setIsLike(score != null);
    }

    @Override
    public Result likeNote(Long id) {
       //获取用户id
        Long userId = TravelerContext.getTraveler().getId();
        // 判断用户是否已点赞
        String key = "cg:like:note:" + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if(score == null){
            //点赞数加1且将用户id加入Redis集合
            Boolean isadd=update().setSql("liked = liked + 1").eq("id", id).update();
            if(isadd){
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        }
        else {
            //点赞数减1且将用户id从Redis集合中移除
            Boolean isremove=update().setSql("liked = liked - 1").eq("id", id).update();
            if(isremove){
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();

    }

    @Override
    public Result queryNoteLikes(Long id) {
        //查询排行前五的用户
        String key = "cg:like:note:" + id;
        Set<String> Top5 = stringRedisTemplate.opsForZSet().reverseRange(key, 0, 4);
        if (Top5 == null || Top5.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
       //解析其中的用户id
        List<Long> userIds = Top5.stream().map(Long::valueOf).collect(Collectors.toList());
        //通过用户id查询用户
        String idStr = StrUtil.join(",", userIds);
        List<UserDTO> userDtos=userService.query()
                .in("id", userIds)
                .last("ORDER BY FIELD(id, " + idStr + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDtos);
    }


}
