## Spring AI 学习指南

### 核心概念

- Model：聊天/嵌入模型，如 OpenAI Chat、Embedding
- Prompt：输入提示，支持消息角色与系统指令
- Response：结构化输出对象，包含文本与元数据
- Vector Store：向量数据库，用于 RAG 检索

### 基本用法（Chat）

```java
// 构造 ChatModel（项目里通过 Spring Bean 提供）
ChatResponse response = chatModel.call("你好，介绍一下Spring AI");
String content = response.getResult().getOutput().getContent();
```

### 基本用法（Embedding）

```java
// 通过 OpenAiEmbeddingModel 生成向量
EmbeddingResponse er = embeddingModel.embed("一段文本");
List<Double> vector = er.getResult().getOutput();
```

### RAG 基本流程

1. 切分原始文档（段落粒度）
2. Embedding 生成向量，写入 Vector Store（pgvector 等）
3. 查询时生成 query 向量，近邻搜索召回文档片段
4. 将召回片段拼装到 Prompt，喂给 ChatModel

### 最佳实践

- 控制模型温度、最大 Token，固定系统指令
- 为产线配置限流/重试/超时，防抖和指数退避
- 对关键上下文做缓存（Caffeine）
- 建设观测（metrics、tracing）和审计日志

### 常见问题

- 429/限流：检查 RateLimiter 与重试策略
- 超时：调大 TimeLimiter 超时或拆分请求
- 成本：控制上下文长度与冗余内容


