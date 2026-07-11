package com.example.agent.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证接口 — 简单登录/注册
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserMapper userMapper;

    public AuthController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 登录
     * POST /api/auth/login
     * Body: {"username":"zhangsan","password":"123456"}
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Map.of("success", false, "message", "用户名和密码不能为空");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));

        if (user == null) {
            return Map.of("success", false, "message", "用户不存在");
        }

        if (!user.getPassword().equals(password)) {
            return Map.of("success", false, "message", "密码错误");
        }

        return Map.of(
                "success", true,
                "userId", user.getUsername(),
                "displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getUsername()
        );
    }

    /**
     * 注册
     * POST /api/auth/register
     * Body: {"username":"wangwu","password":"123456","displayName":"王五"}
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String displayName = body.get("displayName");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Map.of("success", false, "message", "用户名和密码不能为空");
        }

        // 检查是否已存在
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (existing != null) {
            return Map.of("success", false, "message", "用户名已存在");
        }

        User user = new User(username, password,
                displayName != null && !displayName.isBlank() ? displayName : username);
        userMapper.insert(user);

        return Map.of(
                "success", true,
                "userId", user.getUsername(),
                "displayName", user.getDisplayName()
        );
    }
}
