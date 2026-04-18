package com.city.guide.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.city.guide.dto.Result;
import com.city.guide.dto.UserDTO;
import com.city.guide.utils.TravelerContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.city.guide.utils.RedisConstants.LOGIN_USER_KEY;
import static com.city.guide.utils.RedisConstants.LOGIN_USER_TTL;
import static com.city.guide.utils.SystemConstants.SESSION_USER_KEY;

/**
 * 核心认证拦截器：构建 CityGuide 项目的安保屏障
 * 职责：验证旅行者登录状态，并实现旅行者信息在当前线程的透传
 */
public class AuthInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate stringRedisTemplate;
    public AuthInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 去请求头里拿 Token（前端发请求时带过来的身份证）
        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token)) {
            // 没带 Token 肯定没登录，直接拦截
            response.setStatus(401);
            return false;
        }

        // 2. 拿着 Token 去 Redis 里查查有没有对应的用户信息
        String key = LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);

        // 3. 判断 Redis 里有没有这个用户
        if (userMap.isEmpty()) {
            // 查不到说明登录过期了或者 Token 是假的
            response.setStatus(401);
            return false;
        }

        // 4. 把从 Redis 拿到的 Map 重新转回成 UserDTO 对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);

        // 5. 把用户信息存进 ThreadLocal里
        // 这样在后面的 Controller 或 Service 里，不用传参就能直接拿到当前登录人的信息，非常方便
        TravelerContext.saveTraveler(userDTO);

        // 6. 只要用户还在操作，就给 Redis 里的 Token 刷新 30 分钟
        stringRedisTemplate.expire(key, LOGIN_USER_TTL, TimeUnit.MINUTES);

        // 7. 全部通过
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求走完了，一定要把 ThreadLocal 里的用户信息删掉
        // 因为 Tomcat 的线程是会重复利用的，如果不删，下一个人用这个线程时可能会看到你的信息，那就乱套了
        TravelerContext.removeTraveler();
    }
}
