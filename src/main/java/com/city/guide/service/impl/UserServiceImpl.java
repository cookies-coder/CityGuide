package com.city.guide.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.city.guide.utils.RedisConstants.*;
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
        // 1. 检查手机号对不对，不对直接打回
        boolean isPhoneInvalid = RegexUtils.isPhoneInvalid(phone);
        if (isPhoneInvalid) {
            return Result.fail("手机号格式错误，请输入正确的11位大陆手机号");
        }

        // 2. 生成6位数字随机验证码
        String dynamicCode = RandomUtil.randomNumbers(6);

        // 3. 把验证码存到 Redis 缓存里
        // Key 加了前缀方便区分，设置了过期时间（比如2分钟），到期自动删除
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, dynamicCode, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // 4. 在控制台打印一下，模拟发短信了
        log.debug("==> [验证码通知] 手机号: {}, 验证码: {}, 请在2分钟内使用", phone, dynamicCode);

        // 5. 把验证码传回前端
        return Result.ok(dynamicCode);
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String targetPhone = loginForm.getPhone();

        // 1. 再次校验手机号
        if (RegexUtils.isPhoneInvalid(targetPhone)) {
            return Result.fail("手机号码格式不正确");
        }

        // 2. 从 Redis 里面取出刚才存的验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + targetPhone);
        String inputCode = loginForm.getCode();

        // 3. 比较验证码是否一致
        if (cacheCode == null || !cacheCode.equals(inputCode)) {
            return Result.fail("验证码无效或已过期");
        }

        // 4. 去数据库查一下这个用户
        User currentUser = query().eq("phone", targetPhone).one();

        // 5. 如果没注册过，就自动创建一个新用户
        if (currentUser == null) {
            log.debug("新用户登录，正在自动注册...");
            currentUser = initNewUser(targetPhone);
        }

        // 6. 生成一个随机 Token，作为用户的登录令牌
        String token = UUID.randomUUID().toString();

        // 7. 把用户信息存到 Redis，方便后面判断登录状态
        // 这里把 User 对象转成了 UserDTO，只保留 ID、昵称等不敏感信息
        UserDTO userDTO = BeanUtil.copyProperties(currentUser, UserDTO.class);

        // 特别注意：Redis 的 Hash 存对象时，要求里面都是字符串
        // 使用 Hutool 工具类转换一下，防止存 Long 类型字段报错
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((name, value) -> value.toString()));

        // 存入 Redis 并设置过期时间（比如30分钟）
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, userMap);
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);

        // 8. 把 Token 给前端
        return Result.ok(token);
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

