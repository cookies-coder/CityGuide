package com.city.guide.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.city.guide.dto.LoginFormDTO;
import com.city.guide.dto.Result;
import com.city.guide.dto.UserDTO;
import com.city.guide.entity.User;
import com.city.guide.mapper.UserMapper;
import com.city.guide.service.IUserService;
import com.city.guide.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import static com.city.guide.utils.SystemConstants.LOGIN_CODE_KEY;
import static com.city.guide.utils.SystemConstants.SESSION_USER_KEY;


/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @Cookie-coder
 *
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1. 校验手机号格式
        boolean isPhoneInvalid = RegexUtils.isPhoneInvalid(phone);
        if (isPhoneInvalid) {
            return Result.fail("手机号格式错误，请输入正确的11位大陆手机号");
        }

        // 2. 生成6位自定义名称的随机验证码
        String dynamicCode = RandomUtil.randomNumbers(6);

        // 3. 将验证码存入Session
        session.setAttribute(LOGIN_CODE_KEY + phone, dynamicCode);

        // 4. 模拟短信发送日志
        log.debug("==> [CityGuide] 验证码已发送: {}。提醒：验证码30分钟内有效，请勿泄露给他人。", dynamicCode);

        // 5. 返回结果
        return Result.ok(dynamicCode);
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String targetPhone = loginForm.getPhone();

        // 1. 检验手机号是否和之前输入的是否相同
        if (RegexUtils.isPhoneInvalid(targetPhone)) {
            return Result.fail("手机号码格式与之前输入的不一致，请检查后重试");
        }

        // 2. 验证码比对逻辑
        Object cacheCode = session.getAttribute(LOGIN_CODE_KEY + targetPhone);
        String inputCode = loginForm.getCode();

        if (cacheCode == null || !cacheCode.toString().equals(inputCode)) {
            return Result.fail("验证码无效或已过期");
        }

        // 3. 根据手机号检索用户
        User currentUser = query().eq("phone", targetPhone).one();

        // 4. 用户不存在则执行注册流程
        if (currentUser == null) {
            log.debug("检测到新用户，正在为手机号 {} 自动创建账号...", targetPhone);
            currentUser = initNewUser(targetPhone);
        }

        // 5. 将用户信息存入Session
        session.setAttribute(SESSION_USER_KEY, BeanUtil.copyProperties(currentUser, UserDTO.class));

        return Result.ok();
    }



    private User initNewUser(String phone) {
        User newUser = new User();
        newUser.setPhone(phone);
        newUser.setNickName("CG_" + RandomUtil.randomString(8));

        // 写入数据库
        save(newUser);
        log.debug("新用户注册成功，ID: {}", newUser.getId());
        return newUser;
    }
}

