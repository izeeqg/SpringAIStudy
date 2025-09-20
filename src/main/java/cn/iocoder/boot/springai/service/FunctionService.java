package cn.iocoder.boot.springai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 函数调用服务
 * 实现工具调用、函数编排等功能
 */
@Service
public class FunctionService {

    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper;

    public FunctionService(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取当前时间的函数
     */
    public String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 生成随机数的函数
     */
    public Map<String, Object> generateRandomNumber(int min, int max) {
        Random random = new Random();
        int result = random.nextInt(max - min + 1) + min;
        return Map.of(
            "result", result,
            "range", min + " - " + max,
            "timestamp", getCurrentTime()
        );
    }

    /**
     * 计算器函数
     */
    public Map<String, Object> calculate(String expression) {
        try {
            // 简单的四则运算解析（生产环境建议使用专业的表达式解析库）
            String cleanExpr = expression.replaceAll("\\s", "");
            double result = evaluateSimpleExpression(cleanExpr);
            
            return Map.of(
                "expression", expression,
                "result", result,
                "success", true
            );
        } catch (Exception e) {
            return Map.of(
                "expression", expression,
                "error", "计算错误: " + e.getMessage(),
                "success", false
            );
        }
    }

    /**
     * 简单表达式计算（仅支持基本四则运算）
     */
    private double evaluateSimpleExpression(String expr) {
        // 这里是一个简化的实现，生产环境建议使用更强大的表达式解析器
        if (expr.contains("+")) {
            String[] parts = expr.split("\\+");
            return Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]);
        } else if (expr.contains("-")) {
            String[] parts = expr.split("-");
            return Double.parseDouble(parts[0]) - Double.parseDouble(parts[1]);
        } else if (expr.contains("*")) {
            String[] parts = expr.split("\\*");
            return Double.parseDouble(parts[0]) * Double.parseDouble(parts[1]);
        } else if (expr.contains("/")) {
            String[] parts = expr.split("/");
            return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
        }
        return Double.parseDouble(expr);
    }

    /**
     * 带函数调用的对话
     *
     * @param message 用户消息
     * @return 对话结果（可能包含函数调用）
     */
    @RateLimiter(name = "function")
    @Retry(name = "function")
    @TimeLimiter(name = "function")
    public Map<String, Object> chatWithFunctions(String message) {
        try {
            // 定义可用的函数
            List<FunctionCallback> functions = List.of(
                FunctionCallbackWrapper.builder(this::getCurrentTime)
                    .withName("getCurrentTime")
                    .withDescription("获取当前系统时间")
                    .build(),
                
                FunctionCallbackWrapper.builder(this::generateRandomNumber)
                    .withName("generateRandomNumber")
                    .withDescription("生成指定范围内的随机数")
                    .withInputType(RandomNumberRequest.class)
                    .build(),
                
                FunctionCallbackWrapper.builder(this::calculate)
                    .withName("calculate")
                    .withDescription("执行简单的数学计算")
                    .withInputType(CalculateRequest.class)
                    .build()
            );

            // 配置聊天选项，启用函数调用
            OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
                .withFunctions(functions)
                .withModel("gpt-4")
                .withTemperature(0.3F)
                .build();

            // 创建提示并调用
            Prompt prompt = new Prompt(new UserMessage(message), chatOptions);
            ChatResponse response = chatModel.call(prompt);

            return Map.of(
                "message", message,
                "response", response.getResult().getOutput().getContent(),
                "functionCalls", response.getResult().getOutput().getToolCalls() != null ? 
                    response.getResult().getOutput().getToolCalls().size() : 0,
                "model", "gpt-4-with-functions"
            );

        } catch (Exception e) {
            return Map.of(
                "message", message,
                "error", "函数调用失败: " + e.getMessage(),
                "fallback", "请稍后重试或使用普通对话模式"
            );
        }
    }

    /**
     * 获取可用函数列表
     */
    public Map<String, Object> getAvailableFunctions() {
        return Map.of(
            "functions", List.of(
                Map.of(
                    "name", "getCurrentTime",
                    "description", "获取当前系统时间",
                    "parameters", "无参数"
                ),
                Map.of(
                    "name", "generateRandomNumber",
                    "description", "生成指定范围内的随机数",
                    "parameters", "min: 最小值, max: 最大值"
                ),
                Map.of(
                    "name", "calculate",
                    "description", "执行简单的数学计算",
                    "parameters", "expression: 数学表达式（支持 +, -, *, /）"
                )
            ),
            "count", 3,
            "note", "这些函数可以在对话中被AI自动调用"
        );
    }

    // 内部类：函数参数定义
    public static class RandomNumberRequest {
        public int min;
        public int max;
    }

    public static class CalculateRequest {
        public String expression;
    }
}
