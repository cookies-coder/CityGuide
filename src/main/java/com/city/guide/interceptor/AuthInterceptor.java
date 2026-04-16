package com.city.guide.interceptor;

import com.city.guide.dto.UserDTO;
import com.city.guide.utils.TravelerContext;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static com.city.guide.utils.SystemConstants.SESSION_USER_KEY;

/**
 * 核心认证拦截器：构建 CityGuide 项目的安保屏障
 * 职责：验证旅行者登录状态，并实现旅行者信息在当前线程的透传
 */
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取会话容器
        HttpSession session = request.getSession();

        // 2. 尝试从会话中提取脱敏后的旅行者信息
        Object traveler = session.getAttribute(SESSION_USER_KEY);

        // 3. 拦截逻辑：若旅行者未登录，返回 401 状态码（未授权）并阻断后续业务执行
        if (traveler == null) {
            // 这里采用 401 状态码是遵循 RESTful 规范，通知前端引导至登录页
            response.setStatus(401);
            return false;
        }

        // 4. 线程安全处理：将旅行者信息注入 TravelerContext
        // 利用 ThreadLocal 机制，确保在多线程环境下，后续的 Service 层能方便且安全地获取当前登录人
        TravelerContext.saveTraveler((UserDTO) traveler);

        // 5. 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        /*
         * 资源清理：在请求结束后，必须移除 ThreadLocal 中的数据。
         * 核心原因：Tomcat 采用线程池机制，若不手动清理，当前线程在处理下一个请求时可能携带旧旅行者信息，
         * 从而导致内存泄漏或严重的业务数据错乱问题。
         */
        TravelerContext.removeTraveler();
    }
}
