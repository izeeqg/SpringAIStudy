## SpringAI 应用参考实现

一个基于 Spring Boot + Spring AI 的落地项目，包含：

- 架构分层：API 层、应用服务层、AI 适配层
- 性能与观测：限流、重试、超时、指标、Actuator、Prometheus
- 示例能力：对话 API（/api/chat/completion）
- 开发体验：OpenAPI/Swagger UI、集中配置、可扩展 Provider

### 快速开始

1. 准备环境：JDK 17、Maven 3.9+
2. 设置环境变量：
   - `export OPENAI_API_KEY=sk-xxxx`
3. 启动应用：
   - `mvn spring-boot:run`
4. 访问接口：
   - Swagger UI：`http://localhost:8080/swagger-ui.html`
   - 健康检查：`http://localhost:8080/actuator/health`

### 目录结构

```text
src/main/java/cn/iocoder/boot/springai
├── Application.java
├── config
│   ├── OpenAIConfig.java
│   └── SecurityConfig.java
├── controller
│   └── ChatController.java
└── service
    └── ChatService.java
```

### 配置说明（application.yml）

- `spring.ai.openai.api-key`：OpenAI Key（从环境变量读取）
- `resilience4j.*`：限流/重试/超时策略
- `management.*`：Actuator 与 Prometheus 暴露

### 部署建议

- 生产环境配置反向代理（Nginx）与 HTTPS
- 使用进程管理（systemd / Docker）运行
- 通过环境变量或 KMS 托管密钥
- 配置 Prometheus + Grafana 观测

### 后续开发

- 新增 RAG（向量库：pgvector）与函数调用示例
- 增加认证（JWT/OAuth2）与速率限制（IP/用户级）
- 增加审计日志与熔断策略


