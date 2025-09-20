package cn.iocoder.boot.springai.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 示例数据服务
 * 在应用启动时导入一些示例文档到向量库
 */
@Service
public class SampleDataService implements CommandLineRunner {

    @Autowired(required = false)
    private RagService ragService;

    private static final List<Map<String, String>> SAMPLE_DOCUMENTS = List.of(
        Map.of(
            "title", "Spring AI 简介",
            "content", """
                Spring AI 是一个为 Java 开发者提供的 AI 应用开发框架。它提供了统一的 API 来与各种 AI 模型提供商集成，
                包括 OpenAI、Azure OpenAI、Anthropic、Cohere、Mistral AI 等。Spring AI 的核心特性包括：
                1. 统一的 API 抽象，支持多种 AI 模型提供商
                2. 自动配置和 Spring Boot 集成
                3. 向量存储支持，用于实现 RAG（检索增强生成）
                4. 函数调用和工具集成
                5. 可观测性和指标监控
                """
        ),
        Map.of(
            "title", "RAG 架构模式",
            "content", """
                RAG（Retrieval-Augmented Generation）是一种将信息检索与生成式 AI 相结合的架构模式。
                RAG 的工作流程包括：
                1. 文档预处理：将原始文档切分成小块
                2. 向量化：使用嵌入模型将文档块转换为向量
                3. 向量存储：将向量存储在向量数据库中
                4. 检索：根据用户查询检索相关文档块
                5. 增强生成：将检索到的上下文与用户查询一起发送给大语言模型
                这种方式能够让 AI 模型访问最新的、特定领域的知识，提高回答的准确性。
                """
        ),
        Map.of(
            "title", "函数调用最佳实践",
            "content", """
                函数调用（Function Calling）是现代 AI 模型的重要能力，允许模型调用外部工具和 API。
                在 Spring AI 中实现函数调用的最佳实践：
                1. 函数设计：保持函数功能单一、参数清晰
                2. 错误处理：为函数调用添加适当的异常处理
                3. 性能优化：对频繁调用的函数进行缓存
                4. 安全考虑：验证函数参数，防止恶意调用
                5. 监控观测：记录函数调用的指标和日志
                常见的函数调用场景包括：数据查询、计算操作、外部 API 集成、系统状态获取等。
                """
        ),
        Map.of(
            "title", "性能优化策略",
            "content", """
                AI 应用的性能优化是关键考虑因素。主要的优化策略包括：
                1. 缓存策略：对相同或相似的请求结果进行缓存
                2. 连接池管理：合理配置 HTTP 客户端连接池
                3. 限流和熔断：使用 Resilience4j 实现限流、重试、熔断
                4. 异步处理：对于耗时操作使用异步处理
                5. 批量操作：将多个小请求合并为批量请求
                6. 模型选择：根据任务需求选择合适的模型大小
                7. 提示词优化：精简提示词，减少 token 消耗
                8. 监控告警：建立完善的监控和告警机制
                """
        )
    );

    @Override
    public void run(String... args) throws Exception {
        if (ragService == null) {
            System.out.println("RAG 服务未配置，跳过示例数据导入");
            return;
        }

        System.out.println("开始导入示例数据到向量库...");
        
        for (Map<String, String> doc : SAMPLE_DOCUMENTS) {
            try {
                Map<String, Object> metadata = Map.of(
                    "title", doc.get("title"),
                    "source", "系统示例",
                    "type", "documentation",
                    "timestamp", System.currentTimeMillis()
                );
                
                Map<String, Object> result = ragService.addDocument(doc.get("content"), metadata);
                
                if (result.containsKey("success") && (Boolean) result.get("success")) {
                    System.out.println("✓ 已导入: " + doc.get("title"));
                } else {
                    System.out.println("✗ 导入失败: " + doc.get("title") + " - " + result.get("error"));
                }
                
                // 避免频率限制
                Thread.sleep(100);
                
            } catch (Exception e) {
                System.out.println("✗ 导入异常: " + doc.get("title") + " - " + e.getMessage());
            }
        }
        
        System.out.println("示例数据导入完成！");
    }

    /**
     * 手动重新导入示例数据
     */
    public Map<String, Object> reimportSampleData() {
        if (ragService == null) {
            return Map.of(
                "success", false,
                "message", "RAG 服务未配置"
            );
        }

        int successCount = 0;
        int failCount = 0;
        
        for (Map<String, String> doc : SAMPLE_DOCUMENTS) {
            try {
                Map<String, Object> metadata = Map.of(
                    "title", doc.get("title"),
                    "source", "系统示例",
                    "type", "documentation",
                    "timestamp", System.currentTimeMillis()
                );
                
                Map<String, Object> result = ragService.addDocument(doc.get("content"), metadata);
                
                if (result.containsKey("success") && (Boolean) result.get("success")) {
                    successCount++;
                } else {
                    failCount++;
                }
                
            } catch (Exception e) {
                failCount++;
            }
        }
        
        return Map.of(
            "success", failCount == 0,
            "message", String.format("导入完成：成功 %d 个，失败 %d 个", successCount, failCount),
            "successCount", successCount,
            "failCount", failCount,
            "totalCount", SAMPLE_DOCUMENTS.size()
        );
    }

    /**
     * 获取示例文档列表
     */
    public Map<String, Object> getSampleDocuments() {
        return Map.of(
            "documents", SAMPLE_DOCUMENTS.stream().map(doc -> Map.of(
                "title", doc.get("title"),
                "contentLength", doc.get("content").length()
            )).toList(),
            "count", SAMPLE_DOCUMENTS.size()
        );
    }
}
