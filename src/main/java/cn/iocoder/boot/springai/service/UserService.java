package cn.iocoder.boot.springai.service;

import cn.iocoder.boot.springai.entity.User;
import cn.iocoder.boot.springai.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户服务
 * 实现用户管理和认证相关功能
 */
@Service
@Transactional
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Spring Security UserDetailsService 实现
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameAndEnabled(username, true)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在或已禁用: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getValue()))
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * 用户认证
     */
    public Optional<User> authenticate(String username, String rawPassword) {
        Optional<User> userOpt = userRepository.findByUsernameAndEnabled(username, true);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(rawPassword, user.getPassword())) {
                // 更新最后登录时间
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
                return Optional.of(user);
            }
        }
        
        return Optional.empty();
    }

    /**
     * 根据用户名查找用户
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 根据ID查找用户
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * 创建新用户
     */
    public User createUser(String username, String rawPassword, String email, Set<User.Role> roles) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在: " + username);
        }
        
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("邮箱已存在: " + email);
        }

        User user = new User(username, passwordEncoder.encode(rawPassword), email);
        user.setRoles(roles);
        
        return userRepository.save(user);
    }

    /**
     * 更新用户密码
     */
    public void updatePassword(Long userId, String newRawPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        user.setPassword(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
    }

    /**
     * 启用/禁用用户
     */
    public void setUserEnabled(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        user.setEnabled(enabled);
        userRepository.save(user);
    }

    /**
     * 获取用户统计信息
     */
    public Map<String, Object> getUserStats() {
        long totalUsers = userRepository.count();
        long enabledUsers = userRepository.findAll().stream()
                .mapToLong(user -> user.getEnabled() ? 1 : 0)
                .sum();

        return Map.of(
            "totalUsers", totalUsers,
            "enabledUsers", enabledUsers,
            "disabledUsers", totalUsers - enabledUsers
        );
    }

    /**
     * 检查用户是否具有指定角色
     */
    public boolean hasRole(User user, User.Role role) {
        return user.getRoles().contains(role);
    }

    /**
     * 检查用户是否为管理员
     */
    public boolean isAdmin(User user) {
        return hasRole(user, User.Role.ADMIN);
    }
}
