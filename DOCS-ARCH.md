## 架构设计（Architecture）

### 目标

- 清晰分层、可扩展的 AI 应用骨架
- 高可用与韧性：限流、重试、超时、指标
- 良好开发体验与文档化

### 分层与模块

- API 层（controller）：稳定公共接口，输入校验
- 应用层（service）：编排用例逻辑、调用 AI 模型
- 适配层（config/model）：模型配置与 Provider 封装

### 关键技术

- Spring Boot 3.3、Spring AI 1.0.0-M2
- Resilience4j（RateLimiter/Retry/TimeLimiter）
- Actuator + Micrometer + Prometheus
- Spring Security（最小化配置，便于后续接入 JWT/OAuth2）
- SpringDoc OpenAPI（快速调试）

### 非功能设计

- 可观测性：/actuator/*，Prometheus metrics，业务指标埋点
- 性能：连接池与超时（由 Provider SDK 处理）、缓存（Caffeine）
- 可扩展：通过 Bean 条件化/配置切换不同模型 Provider

### 路线图

1. 对话与健康检查（已完成）
2. RAG：向量库 + 文档导入 + 检索增强
3. 工具/函数调用：结构化工具编排
4. 安全：JWT + 速率限制 + 审计
5. 前端示例：简易对话 UI + SSE 流式输出


