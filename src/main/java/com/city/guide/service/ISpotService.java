package com.city.guide.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.city.guide.dto.Result;
import com.city.guide.entity.Spot;

/**
 * <p>
 * 景点服务类
 * </p>
 *
 * @Cookie-coder
 * 
 */
public interface ISpotService extends IService<Spot> {


    Result queryById(Long id);
}
