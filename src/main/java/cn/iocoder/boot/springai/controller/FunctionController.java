package cn.iocoder.boot.springai.controller;

import cn.iocoder.boot.springai.service.FunctionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 函数调用控制器
 */
@RestController
@RequestMapping("/api/functions")
@Validated
@Tag(name = "Function API", description = "函数调用和工具调用相关接口")
public class FunctionController {

    private final FunctionService functionService;

    public FunctionController(FunctionService functionService) {
        this.functionService = functionService;
    }

    /**
     * 带函数调用的对话
     */
    @PostMapping(path = "/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "函数调用对话", description = "AI可以在对话中自动调用可用函数")
    public Map<String, Object> chatWithFunctions(@RequestParam @NotBlank String message) {
        return functionService.chatWithFunctions(message);
    }

    /**
     * 获取可用函数列表
     */
    @GetMapping(path = "/available", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "获取可用函数", description = "查看当前系统中可供AI调用的函数列表")
    public Map<String, Object> getAvailableFunctions() {
        return functionService.getAvailableFunctions();
    }

    /**
     * 直接调用时间函数
     */
    @GetMapping(path = "/time", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "获取当前时间", description = "直接调用时间获取函数")
    public Map<String, Object> getCurrentTime() {
        return Map.of(
            "currentTime", functionService.getCurrentTime(),
            "timezone", "Asia/Shanghai"
        );
    }

    /**
     * 直接调用随机数生成函数
     */
    @PostMapping(path = "/random", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "生成随机数", description = "生成指定范围内的随机数")
    public Map<String, Object> generateRandomNumber(
            @RequestParam(defaultValue = "1") int min,
            @RequestParam(defaultValue = "100") int max) {
        return functionService.generateRandomNumber(min, max);
    }

    /**
     * 直接调用计算函数
     */
    @PostMapping(path = "/calculate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "数学计算", description = "执行简单的数学运算")
    public Map<String, Object> calculate(@RequestParam @NotBlank String expression) {
        return functionService.calculate(expression);
    }

    /**
     * 健康检查
     */
    @GetMapping(path = "/health")
    @Operation(summary = "健康检查", description = "检查函数调用服务状态")
    public Map<String, Object> health() {
        return Map.of(
            "status", "ok",
            "service", "functions",
            "availableFunctions", 3,
            "timestamp", System.currentTimeMillis()
        );
    }
}
