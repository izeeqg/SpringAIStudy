package cn.iocoder.boot.springai.controller;

import cn.iocoder.boot.springai.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * RAG检索增强生成控制器
 */
@RestController
@RequestMapping("/api/rag")
@Validated
@Tag(name = "RAG API", description = "检索增强生成相关接口")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * 添加文档到向量库
     */
    @PostMapping(path = "/documents", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "添加文档", description = "将文档内容向量化并存储到向量库")
    public Map<String, Object> addDocument(
            @RequestParam @NotBlank String content,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String source) {
        
        Map<String, Object> metadata = Map.of(
            "title", title != null ? title : "未命名文档",
            "source", source != null ? source : "用户上传",
            "timestamp", System.currentTimeMillis()
        );
        
        return ragService.addDocument(content, metadata);
    }

    /**
     * RAG检索增强问答
     */
    @PostMapping(path = "/query", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "RAG问答", description = "基于向量库检索相关文档，生成增强回答")
    public Map<String, Object> ragQuery(
            @RequestParam @NotBlank String question,
            @RequestParam(defaultValue = "3") Integer topK) {
        
        return ragService.ragQuery(question, topK);
    }

    /**
     * 检索相似文档
     */
    @GetMapping(path = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "文档检索", description = "根据查询文本检索相似文档")
    public Map<String, Object> searchDocuments(
            @RequestParam @NotBlank String query,
            @RequestParam(defaultValue = "5") Integer topK) {
        
        return ragService.searchDocuments(query, topK);
    }

    /**
     * 健康检查
     */
    @GetMapping(path = "/health")
    @Operation(summary = "健康检查", description = "检查RAG服务状态")
    public Map<String, Object> health() {
        return Map.of(
            "status", "ok",
            "service", "rag",
            "timestamp", System.currentTimeMillis()
        );
    }
}
