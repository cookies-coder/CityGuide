package com.city.guide.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.city.guide.entity.UserInfo;
import com.city.guide.mapper.UserInfoMapper;
import com.city.guide.service.IUserInfoService;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @Cookie-coder
 * @since 2021-12-24
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}

