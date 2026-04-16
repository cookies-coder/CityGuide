package com.city.guide.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.city.guide.dto.Result;
import com.city.guide.entity.SpotType;

/**
 * <p>
 * 景点类型服务类
 * </p>
 *
 * @Cookie-coder
 * 
 */
public interface ISpotTypeService extends IService<SpotType> {

    Result queryTypeList();
}
