package cn.iocoder.boot.springai.service;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG检索增强生成服务
 * 实现文档向量化存储、相似度检索、上下文增强对话
 */
@Service
public class RagService {

    private final OpenAiChatModel chatModel;
    private final EmbeddingModel embeddingModel;
    
    @Autowired(required = false)
    private VectorStore vectorStore;

    private static final String RAG_PROMPT_TEMPLATE = """
            基于以下上下文信息回答用户问题。如果上下文中没有相关信息，请明确说明。
            
            上下文：
            {context}
            
            问题：{question}
            
            回答：
            """;

    public RagService(OpenAiChatModel chatModel, EmbeddingModel embeddingModel) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
    }

    /**
     * 添加文档到向量库
     *
     * @param content 文档内容
     * @param metadata 文档元数据
     * @return 添加结果
     */
    public Map<String, Object> addDocument(String content, Map<String, Object> metadata) {
        if (vectorStore == null) {
            return Map.of("error", "向量存储未配置");
        }

        try {
            Document document = new Document(content, metadata);
            vectorStore.add(List.of(document));
            
            return Map.of(
                "success", true,
                "message", "文档已添加到向量库",
                "contentLength", content.length()
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "error", "添加文档失败: " + e.getMessage()
            );
        }
    }

    /**
     * RAG检索增强问答
     *
     * @param question 用户问题
     * @param topK 检索文档数量
     * @return 增强回答结果
     */
    @RateLimiter(name = "rag")
    @Retry(name = "rag")
    @TimeLimiter(name = "rag")
    public Map<String, Object> ragQuery(String question, Integer topK) {
        if (vectorStore == null) {
            // 降级到普通对话
            ChatResponse response = chatModel.call(question);
            return Map.of(
                "question", question,
                "answer", response.getResult().getOutput().getContent(),
                "mode", "fallback_chat",
                "context", "向量存储未配置，使用普通对话模式"
            );
        }

        try {
            // 1. 向量检索相关文档
            SearchRequest searchRequest = SearchRequest.query(question)
                    .withTopK(topK != null ? topK : 3)
                    .withSimilarityThreshold(0.7);
            
            List<Document> similarDocs = vectorStore.similaritySearch(searchRequest);
            
            // 2. 构建上下文
            String context = similarDocs.stream()
                    .map(Document::getContent)
                    .collect(Collectors.joining("\n\n"));
            
            // 3. 构建增强提示词
            PromptTemplate promptTemplate = new PromptTemplate(RAG_PROMPT_TEMPLATE);
            Prompt prompt = promptTemplate.create(Map.of(
                "context", context,
                "question", question
            ));
            
            // 4. 生成回答
            ChatResponse response = chatModel.call(prompt);
            
            return Map.of(
                "question", question,
                "answer", response.getResult().getOutput().getContent(),
                "mode", "rag",
                "retrievedDocs", similarDocs.size(),
                "context", context.substring(0, Math.min(context.length(), 200)) + "..."
            );
            
        } catch (Exception e) {
            return Map.of(
                "question", question,
                "error", "RAG查询失败: " + e.getMessage(),
                "mode", "error"
            );
        }
    }

    /**
     * 检索相似文档（不生成回答）
     *
     * @param query 查询文本
     * @param topK 返回文档数量
     * @return 相似文档列表
     */
    public Map<String, Object> searchDocuments(String query, Integer topK) {
        if (vectorStore == null) {
            return Map.of("error", "向量存储未配置");
        }

        try {
            SearchRequest searchRequest = SearchRequest.query(query)
                    .withTopK(topK != null ? topK : 5)
                    .withSimilarityThreshold(0.6);
            
            List<Document> documents = vectorStore.similaritySearch(searchRequest);
            
            return Map.of(
                "query", query,
                "results", documents.stream().map(doc -> Map.of(
                    "content", doc.getContent(),
                    "metadata", doc.getMetadata()
                )).collect(Collectors.toList()),
                "count", documents.size()
            );
            
        } catch (Exception e) {
            return Map.of(
                "query", query,
                "error", "文档检索失败: " + e.getMessage()
            );
        }
    }
}
