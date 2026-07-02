package com.city.guide.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.city.guide.dto.LoginFormDTO;
import com.city.guide.dto.Result;
import com.city.guide.entity.User;
import com.city.guide.mapper.UserMapper;
import com.city.guide.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import static com.city.guide.utils.RedisConstants.LOGIN_CODE_KEY;
import static com.city.guide.utils.RedisConstants.LOGIN_CODE_TTL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private HttpSession session;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        // 反射设置 MyBatis Plus 的 baseMapper
        Field field = ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(userService, userMapper);
    }

    @Test
    void testSendCode_ValidPhone_ShouldReturnCode() {
        String phone = "13800138000";

        Result result = userService.sendCode(phone, session);

        assertTrue(result.getSuccess());
        assertNotNull(result.getData());
        String code = (String) result.getData();
        assertEquals(6, code.length());

        verify(valueOperations, times(1))
                .set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
    }

    @Test
    void testSendCode_EmptyPhone_ShouldReturnError() {
        String phone = "";

        Result result = userService.sendCode(phone, session);

        assertFalse(result.getSuccess());
        assertNotNull(result.getErrorMsg());

        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void testSendCode_InvalidPhone_ShouldReturnError() {
        String phone = "abc123";

        Result result = userService.sendCode(phone, session);

        assertFalse(result.getSuccess());
    }

    @Test
    void testLogin_ValidCodeAndNewUser_ShouldAutoRegister() {
        String phone = "13900001111";
        String code = "666666";

        when(valueOperations.get(LOGIN_CODE_KEY + phone)).thenReturn(code);
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.insert(any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return 1;
        });

        LoginFormDTO loginForm = new LoginFormDTO();
        loginForm.setPhone(phone);
        loginForm.setCode(code);

        Result result = userService.login(loginForm, session);

        assertTrue(result.getSuccess());
        assertNotNull(result.getData());
        String token = (String) result.getData();
        assertFalse(token.isEmpty());

        verify(userMapper, times(1)).selectOne(any());
        verify(userMapper, times(1)).insert(any());
    }

    @Test
    void testLogin_WrongCode_ShouldReturnError() {
        String phone = "13800138000";

        when(valueOperations.get(LOGIN_CODE_KEY + phone)).thenReturn("123456");

        LoginFormDTO loginForm = new LoginFormDTO();
        loginForm.setPhone(phone);
        loginForm.setCode("999999");

        Result result = userService.login(loginForm, session);

        assertFalse(result.getSuccess());
        assertEquals("验证码无效或已过期", result.getErrorMsg());
    }

    @Test
    void testLogin_ExpiredCode_ShouldReturnError() {
        String phone = "13800138000";

        when(valueOperations.get(LOGIN_CODE_KEY + phone)).thenReturn(null);

        LoginFormDTO loginForm = new LoginFormDTO();
        loginForm.setPhone(phone);
        loginForm.setCode("666666");

        Result result = userService.login(loginForm, session);

        assertFalse(result.getSuccess());
        assertEquals("验证码无效或已过期", result.getErrorMsg());
    }

    @Test
    void testLogin_InvalidPhone_ShouldReturnError() {
        LoginFormDTO loginForm = new LoginFormDTO();
        loginForm.setPhone("1234");
        loginForm.setCode("666666");

        Result result = userService.login(loginForm, session);

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMsg().contains("手机号"));
    }
}
