package com.city.guide.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.city.guide.dto.Result;
import com.city.guide.entity.GuideNote;
import com.city.guide.service.IGuideNoteService;
import com.city.guide.utils.SystemConstants;
import com.city.guide.utils.TravelerContext;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 旅行笔记前端控制器
 * </p>
 *
 * @Cookie-coder
 * 
 */
@RestController
@RequestMapping("/guide-note")
public class GuideNoteController {

    @Resource
    private IGuideNoteService guideNoteService;

    @PostMapping
    public Result saveGuideNote(@RequestBody GuideNote guideNote) {
        Long id = guideNoteService.saveGuideNote(guideNote);
        return Result.ok(guideNote.getId());
    }

    @PutMapping("/like/{id}")
    public Result likeGuideNote(@PathVariable("id") Long id) {

        return guideNoteService.likeNote(id);
    }

    @PutMapping("/likes/{id}")
    public Result queryNoteLikes(@PathVariable("id") Long id) {

        return guideNoteService.queryNoteLikes(id);
    }

    @GetMapping("/of/me")
    public Result queryMyGuideNote(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        List<GuideNote> records = guideNoteService.queryGuideNote(current);
        return Result.ok(records);
    }

    @GetMapping("/hot")
    public Result queryHotGuideNote(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return guideNoteService.queryHotGuideNote(current);
    }

    @GetMapping("/{id}")
    public Result queryGuideNoteById(@PathVariable("id") Long id) {
        return guideNoteService.queryGuideNoteById(id);
    }
}
