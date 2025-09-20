# SpringAI 应用部署指南

## 环境要求

- **JDK**: 17 或更高版本
- **Maven**: 3.9+ 
- **内存**: 推荐 2GB 以上
- **存储**: 500MB 以上可用空间
- **网络**: 需要访问 OpenAI API（或其他配置的 AI 服务）

## 配置说明

### 必需配置

1. **OpenAI API Key**
   ```bash
   export OPENAI_API_KEY="sk-your-openai-api-key"
   ```

2. **应用配置** (`application.yml`)
   ```yaml
   spring:
     ai:
       openai:
         api-key: ${OPENAI_API_KEY}
         base-url: https://api.openai.com  # 可选：自定义 API 地址
   ```

### 可选配置

1. **向量数据库** (pgvector)
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/vectordb
       username: postgres
       password: password
   ```

2. **缓存调优**
   ```yaml
   # 在 application.yml 中无需额外配置，使用代码中的 Caffeine 默认配置
   ```

3. **监控配置**
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: "health,info,metrics,prometheus"
   ```

## 部署方式

### 1. 开发环境

```bash
# 克隆项目
git clone <your-repo-url>
cd SpringAI

# 设置环境变量
export OPENAI_API_KEY="sk-your-key"

# 启动应用
mvn spring-boot:run
```

访问地址：
- 应用首页: http://localhost:8080
- API 文档: http://localhost:8080/swagger-ui.html
- 健康检查: http://localhost:8080/actuator/health

### 2. 生产环境 (JAR)

```bash
# 构建
mvn clean package -DskipTests

# 运行
java -jar target/springai-app-0.1.0.jar \
  --spring.profiles.active=prod \
  --server.port=8080
```

### 3. Docker 部署

创建 `Dockerfile`:
```dockerfile
FROM openjdk:17-jre-slim

WORKDIR /app
COPY target/springai-app-0.1.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

构建和运行:
```bash
# 构建镜像
docker build -t springai-app .

# 运行容器
docker run -d \
  -p 8080:8080 \
  -e OPENAI_API_KEY="sk-your-key" \
  --name springai-app \
  springai-app
```

### 4. Docker Compose

创建 `docker-compose.yml`:
```yaml
version: '3.8'
services:
  springai-app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - OPENAI_API_KEY=sk-your-key
      - SPRING_PROFILES_ACTIVE=prod
    restart: unless-stopped
    
  # 可选：添加 PostgreSQL (用于向量存储)
  postgres:
    image: pgvector/pgvector:pg16
    environment:
      - POSTGRES_DB=vectordb
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

启动:
```bash
docker-compose up -d
```

## 监控和观测

### 1. 健康检查

```bash
# 应用健康状态
curl http://localhost:8080/actuator/health

# 系统状态
curl http://localhost:8080/api/management/status
```

### 2. 指标监控

```bash
# Prometheus 指标
curl http://localhost:8080/actuator/prometheus

# 业务指标
curl http://localhost:8080/actuator/metrics
```

### 3. 日志配置

在 `application-prod.yml` 中:
```yaml
logging:
  level:
    cn.iocoder.boot.springai: INFO
    org.springframework.ai: DEBUG
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/springai-app.log
```

## 性能调优

### 1. JVM 参数

```bash
java -jar app.jar \
  -Xms1g -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+PrintGC
```

### 2. 连接池调优

```yaml
spring:
  task:
    execution:
      pool:
        core-size: 8
        max-size: 16
        queue-capacity: 100
```

### 3. 缓存优化

检查缓存命中率:
```bash
curl http://localhost:8080/api/management/cache/stats
```

## 安全配置

### 1. HTTPS 配置

```yaml
server:
  ssl:
    key-store: classpath:keystore.p12
    key-store-password: password
    key-store-type: PKCS12
  port: 8443
```

### 2. 认证配置

```yaml
spring:
  security:
    user:
      name: admin
      password: {bcrypt}$2a$10$...  # 使用 bcrypt 加密
```

### 3. 反向代理 (Nginx)

```nginx
upstream springai-app {
    server localhost:8080;
}

server {
    listen 80;
    server_name your-domain.com;
    
    location / {
        proxy_pass http://springai-app;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## 故障排查

### 1. 常见问题

**问题**: 无法连接 OpenAI API
```bash
# 检查网络连接
curl -I https://api.openai.com

# 检查 API Key
curl -H "Authorization: Bearer $OPENAI_API_KEY" \
  https://api.openai.com/v1/models
```

**问题**: 内存不足
```bash
# 检查内存使用
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# 调整堆内存
export JAVA_OPTS="-Xmx2g"
```

### 2. 日志分析

```bash
# 查看应用日志
tail -f logs/springai-app.log

# 查看错误日志
grep ERROR logs/springai-app.log
```

### 3. 性能分析

```bash
# 查看响应时间
curl http://localhost:8080/actuator/metrics/http.server.requests

# 查看 AI 调用统计
curl http://localhost:8080/actuator/metrics/ai.calls.total
```

## 备份和恢复

### 1. 配置备份

```bash
# 备份配置文件
cp application*.yml backup/
```

### 2. 向量数据备份 (如使用 pgvector)

```bash
# 导出数据
pg_dump -h localhost -U postgres vectordb > vectordb_backup.sql

# 恢复数据
psql -h localhost -U postgres vectordb < vectordb_backup.sql
```

## 扩展和集成

### 1. 集群部署

使用负载均衡器分发请求到多个应用实例：

```yaml
# docker-compose.yml
services:
  springai-app-1:
    build: .
    ports:
      - "8081:8080"
  springai-app-2:
    build: .
    ports:
      - "8082:8080"
  nginx:
    image: nginx
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
```

### 2. 外部监控

集成 Prometheus + Grafana:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'springai-app'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
```

通过以上部署指南，您可以在不同环境中成功部署和运行 SpringAI 应用。
