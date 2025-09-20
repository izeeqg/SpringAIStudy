package cn.iocoder.boot.springai.service;

import cn.iocoder.boot.springai.entity.User;
import cn.iocoder.boot.springai.security.JwtTokenProvider;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 认证服务
 * 处理用户登录、token生成和刷新等认证相关业务
 */
@Service
public class AuthService {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserService userService, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录结果，包含token信息
     */
    public Map<String, Object> login(String username, String password) {
        Optional<User> userOpt = userService.authenticate(username, password);
        
        if (userOpt.isEmpty()) {
            return Map.of(
                "success", false,
                "message", "用户名或密码错误"
            );
        }

        User user = userOpt.get();
        
        // 生成token
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return Map.of(
            "success", true,
            "message", "登录成功",
            "accessToken", accessToken,
            "refreshToken", refreshToken,
            "expiresIn", jwtTokenProvider.getAccessTokenValidityMs() / 1000, // 转换为秒
            "user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "roles", user.getRoles(),
                "lastLogin", user.getLastLogin()
            )
        );
    }

    /**
     * 刷新token
     *
     * @param refreshToken 刷新token
     * @return 新的访问token
     */
    public Map<String, Object> refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            return Map.of(
                "success", false,
                "message", "刷新token无效或已过期"
            );
        }

        try {
            String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
            Optional<User> userOpt = userService.findByUsername(username);
            
            if (userOpt.isEmpty()) {
                return Map.of(
                    "success", false,
                    "message", "用户不存在"
                );
            }

            User user = userOpt.get();
            if (!user.getEnabled()) {
                return Map.of(
                    "success", false,
                    "message", "用户已被禁用"
                );
            }

            // 生成新的访问token
            String newAccessToken = jwtTokenProvider.generateAccessToken(user);

            return Map.of(
                "success", true,
                "message", "token刷新成功",
                "accessToken", newAccessToken,
                "expiresIn", jwtTokenProvider.getAccessTokenValidityMs() / 1000
            );

        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "token刷新失败: " + e.getMessage()
            );
        }
    }

    /**
     * 验证token
     *
     * @param token 访问token
     * @return 验证结果
     */
    public Map<String, Object> validateToken(String token) {
        if (!jwtTokenProvider.validateAccessToken(token)) {
            return Map.of(
                "valid", false,
                "message", "token无效或已过期"
            );
        }

        try {
            String username = jwtTokenProvider.getUsernameFromToken(token);
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            
            return Map.of(
                "valid", true,
                "username", username,
                "userId", userId,
                "roles", jwtTokenProvider.getRolesFromToken(token),
                "expiresAt", jwtTokenProvider.getExpirationFromToken(token)
            );

        } catch (Exception e) {
            return Map.of(
                "valid", false,
                "message", "token解析失败: " + e.getMessage()
            );
        }
    }

    /**
     * 用户注册
     *
     * @param username 用户名
     * @param password 密码
     * @param email 邮箱
     * @return 注册结果
     */
    public Map<String, Object> register(String username, String password, String email) {
        try {
            User user = userService.createUser(username, password, email, 
                Set.of(User.Role.USER)); // 默认为普通用户角色

            return Map.of(
                "success", true,
                "message", "注册成功",
                "user", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "roles", user.getRoles(),
                    "createdAt", user.getCreatedAt()
                )
            );

        } catch (IllegalArgumentException e) {
            return Map.of(
                "success", false,
                "message", e.getMessage()
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "注册失败: " + e.getMessage()
            );
        }
    }

    /**
     * 获取当前用户信息
     *
     * @param token 访问token
     * @return 用户信息
     */
    public Map<String, Object> getCurrentUser(String token) {
        if (!jwtTokenProvider.validateAccessToken(token)) {
            return Map.of(
                "success", false,
                "message", "token无效"
            );
        }

        try {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            Optional<User> userOpt = userService.findById(userId);
            
            if (userOpt.isEmpty()) {
                return Map.of(
                    "success", false,
                    "message", "用户不存在"
                );
            }

            User user = userOpt.get();
            return Map.of(
                "success", true,
                "user", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "roles", user.getRoles(),
                    "enabled", user.getEnabled(),
                    "createdAt", user.getCreatedAt(),
                    "lastLogin", user.getLastLogin()
                )
            );

        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "获取用户信息失败: " + e.getMessage()
            );
        }
    }
}
