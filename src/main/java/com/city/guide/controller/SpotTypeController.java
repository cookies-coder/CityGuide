package com.city.guide.controller;


import com.city.guide.dto.Result;
import com.city.guide.entity.SpotType;
import com.city.guide.service.ISpotTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 景点类型前端控制器
 * </p>
 *
 * @Cookie-coder
 * 
 */
@RestController
@RequestMapping("/spot-type")
public class SpotTypeController {
    @Resource
    private ISpotTypeService typeService;

    @GetMapping("list")
    public Result queryTypeList() {
        return typeService.queryTypeList();
    }
}
