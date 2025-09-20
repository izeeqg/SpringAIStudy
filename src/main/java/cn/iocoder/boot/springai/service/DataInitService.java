package cn.iocoder.boot.springai.service;

import cn.iocoder.boot.springai.entity.User;
import cn.iocoder.boot.springai.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 数据初始化服务
 * 在应用启动时创建默认用户和数据
 */
@Service
@Order(1) // 优先级高于SampleDataService
public class DataInitService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    public DataInitService(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public void run(String... args) throws Exception {
        initDefaultUsers();
    }

    /**
     * 初始化默认用户
     */
    private void initDefaultUsers() {
        // 创建默认管理员用户
        if (!userRepository.existsByUsername("admin")) {
            try {
                User admin = userService.createUser(
                    "admin",
                    "admin123",
                    "admin@springai.com",
                    Set.of(User.Role.ADMIN, User.Role.USER)
                );
                System.out.println("✓ 已创建默认管理员用户: admin / admin123");
            } catch (Exception e) {
                System.out.println("✗ 创建管理员用户失败: " + e.getMessage());
            }
        }

        // 创建默认普通用户
        if (!userRepository.existsByUsername("user")) {
            try {
                User user = userService.createUser(
                    "user",
                    "user123",
                    "user@springai.com",
                    Set.of(User.Role.USER)
                );
                System.out.println("✓ 已创建默认普通用户: user / user123");
            } catch (Exception e) {
                System.out.println("✗ 创建普通用户失败: " + e.getMessage());
            }
        }

        // 创建API用户
        if (!userRepository.existsByUsername("apiuser")) {
            try {
                User apiUser = userService.createUser(
                    "apiuser",
                    "api123",
                    "api@springai.com",
                    Set.of(User.Role.API_USER, User.Role.USER)
                );
                System.out.println("✓ 已创建API用户: apiuser / api123");
            } catch (Exception e) {
                System.out.println("✗ 创建API用户失败: " + e.getMessage());
            }
        }

        System.out.println("用户初始化完成！");
        System.out.println("管理员登录: admin / admin123");
        System.out.println("普通用户登录: user / user123");
        System.out.println("API用户登录: apiuser / api123");
    }
}
