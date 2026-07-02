package com.city.guide.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.city.guide.dto.Result;
import com.city.guide.entity.Spot;
import com.city.guide.service.ISpotService;
import com.city.guide.utils.Gcj02ToBd09Converter;
import com.city.guide.utils.SystemConstants;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 景点前端控制器
 * </p>
 *
 * @Cookie-coder
 * 
 */
@RestController
@RequestMapping("/spot")
public class SpotController {

    @Resource
    public ISpotService spotService;

    /**
     * 根据id查询景点信息
     * @param id 景点id
     * @return 景点详情数据
     */
    @GetMapping("/{id}")
    public Result querySpotById(@PathVariable("id") Long id) {

        return spotService.queryById(id);
    }

    /**
     * 新增景点信息
     * @param spot 景点数据
     * @return 景点id
     */
    @PostMapping
    public Result saveSpot(@RequestBody Spot spot) {
        // 写入数据库
        spotService.save(spot);
        // 返回景点id
        return Result.ok(spot.getId());
    }

    /**
     * 更新景点信息
     * @param spot 景点数据
     * @return 无
     */
    @PutMapping
    public Result updateSpot(@RequestBody Spot spot) {
        return spotService.updata(spot);
    }

    /**
     * 根据景点类型分页查询景点信息
     * @param typeId 景点类型
     * @param current 页码
     * @return 景点列表
     */
    @GetMapping("/of/type")
    public Result querySpotByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据类型分页查询
        Page<Spot> page = spotService.query()
                .eq("type_id", typeId)
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        
        // 为每个景点转换坐标
        List<Spot> spotList = page.getRecords().stream().map(spot -> {
            double[] bdCoord = Gcj02ToBd09Converter.gcj02ToBd09(spot.getX(), spot.getY());
            spot.setBdX(bdCoord[0]);
            spot.setBdY(bdCoord[1]);
            return spot;
        }).collect(Collectors.toList());
        
        // 返回数据
        return Result.ok(spotList);
    }

    /**
     * 根据景点名称关键字分页查询景点信息
     * @param name 景点名称关键字
     * @param current 页码
     * @return 景点列表
     */
    @GetMapping("/of/name")
    public Result querySpotByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // 根据类型分页查询
        Page<Spot> page = spotService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        
        // 为每个景点转换坐标
        List<Spot> spotList = page.getRecords().stream().map(spot -> {
            double[] bdCoord = Gcj02ToBd09Converter.gcj02ToBd09(spot.getX(), spot.getY());
            spot.setBdX(bdCoord[0]);
            spot.setBdY(bdCoord[1]);
            return spot;
        }).collect(Collectors.toList());
        
        // 返回数据
        return Result.ok(spotList);
    }
}
