package com.city.guide.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.city.guide.dto.Result;
import com.city.guide.dto.UserDTO;
import com.city.guide.entity.GuideNote;
import com.city.guide.entity.NoteComment;
import com.city.guide.mapper.NoteCommentMapper;
import com.city.guide.service.IGuideNoteService;
import com.city.guide.service.INoteCommentService;
import com.city.guide.utils.TravelerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 笔记评论服务实现类
 * </p>
 *
 * @Cookie-coder
 * 
 */
@Slf4j
@Service
public class NoteCommentServiceImpl extends ServiceImpl<NoteCommentMapper, NoteComment> implements INoteCommentService {

    private static final String NOTE_COMMENT_LIKE_KEY = "cg:like:note:comment:";

    @Resource
    private IGuideNoteService guideNoteService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发布评论（一级评论或子评论）
     * 发布后同步更新笔记评论计数
     */
    @Override
    @Transactional
    public Result saveNoteComment(NoteComment noteComment) {
        UserDTO traveler = TravelerContext.getTraveler();
        if (traveler == null) {
            return Result.fail("请先登录");
        }

        // 校验笔记是否存在
        Long noteId = noteComment.getNoteId();
        GuideNote note = guideNoteService.getById(noteId);
        if (note == null) {
            return Result.fail("笔记不存在");
        }

        // 校验子评论时父评论是否存在且属于同一篇笔记
        Long parentId = noteComment.getParentId();
        if (parentId != null && parentId != 0L) {
            NoteComment parent = getById(parentId);
            if (parent == null) {
                return Result.fail("父评论不存在");
            }
            if (!parent.getNoteId().equals(noteId)) {
                return Result.fail("父评论不属于该笔记");
            }
        }

        // 填充字段
        noteComment.setUserId(traveler.getId());
        noteComment.setCreateTime(LocalDateTime.now());
        noteComment.setUpdateTime(LocalDateTime.now());
        noteComment.setLiked(0);
        noteComment.setStatus(false);

        boolean saved = save(noteComment);
        if (!saved) {
            return Result.fail("评论发布失败");
        }

        // 笔记评论数 +1
        guideNoteService.update().setSql("comments = comments + 1")
                .eq("id", noteId).update();

        return Result.ok(noteComment.getId());
    }

    /**
     * 删除评论
     * 只允许本人操作；一级评论连带删除所有子评论；同步更新笔记评论计数
     */
    @Override
    @Transactional
    public Result deleteNoteComment(Long commentId) {
        UserDTO traveler = TravelerContext.getTraveler();
        if (traveler == null) {
            return Result.fail("请先登录");
        }

        NoteComment comment = getById(commentId);
        if (comment == null) {
            return Result.fail("评论不存在");
        }

        // 只允许本人删除
        if (!comment.getUserId().equals(traveler.getId())) {
            return Result.fail("只能删除自己的评论");
        }

        int deletedCount = 0;

        if (comment.getParentId() == 0L) {
            // 一级评论：先查子评论数量，再连同删除
            Integer replyCount = baseMapper.countRepliesByParentId(commentId);
            int childCount = (replyCount != null) ? replyCount : 0;

            // 删除子评论
            if (childCount > 0) {
                lambdaUpdate().eq(NoteComment::getParentId, commentId).remove();
            }
            // 删除一级评论本身
            removeById(commentId);

            deletedCount = 1 + childCount;
        } else {
            // 子评论：直接删除
            removeById(commentId);
            deletedCount = 1;
        }

        // 笔记评论数 - deletedCount
        guideNoteService.update().setSql("comments = comments - " + deletedCount)
                .eq("id", comment.getNoteId()).update();

        return Result.ok("删除成功");
    }

    /**
     * 分页查询指定笔记的一级评论（按时间倒序，最新在前）
     */
    @Override
    public Result queryTopComments(Long noteId, Integer current, Integer size) {
        Page<NoteComment> page = new Page<>(current, size);
        Page<NoteComment> resultPage = baseMapper.queryTopComments(page, noteId);
        resultPage.getRecords().forEach(this::fillIsLike);
        return Result.ok(resultPage);
    }

    /**
     * 分页查询指定一级评论下的子评论（按时间正序，早期在前）
     */
    @Override
    public Result queryReplies(Long parentId, Integer current, Integer size) {
        Page<NoteComment> page = new Page<>(current, size);
        Page<NoteComment> resultPage = baseMapper.queryReplies(page, parentId);
        resultPage.getRecords().forEach(this::fillIsLike);
        return Result.ok(resultPage);
    }

    /**
     * 点赞/取消点赞评论（切换逻辑）
     * Redis ZSet 存储：key = cg:like:note:comment:{commentId}，member = userId，score = 时间戳
     */
    @Override
    public Result likeComment(Long commentId) {
        UserDTO traveler = TravelerContext.getTraveler();
        if (traveler == null) {
            return Result.fail("请先登录");
        }

        Long userId = traveler.getId();
        String key = NOTE_COMMENT_LIKE_KEY + commentId;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());

        if (score == null) {
            // 未点赞 → 点赞：数据库 liked +1，Redis ZADD
            update().setSql("liked = liked + 1").eq("id", commentId).update();
            stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
        } else {
            // 已点赞 → 取消点赞：数据库 liked -1，Redis ZREM
            update().setSql("liked = liked - 1").eq("id", commentId).update();
            stringRedisTemplate.opsForZSet().remove(key, userId.toString());
        }

        return Result.ok();
    }

    /**
     * 填充当前用户是否点赞过该评论
     */
    private void fillIsLike(NoteComment comment) {
        UserDTO traveler = TravelerContext.getTraveler();
        if (traveler == null) {
            comment.setIsLike(false);
            return;
        }
        String key = NOTE_COMMENT_LIKE_KEY + comment.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key, traveler.getId().toString());
        comment.setIsLike(score != null);
    }

}
