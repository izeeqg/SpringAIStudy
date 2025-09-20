package cn.iocoder.boot.springai.controller;

import cn.iocoder.boot.springai.ratelimit.RateLimit;
import cn.iocoder.boot.springai.ratelimit.RateLimitType;
import cn.iocoder.boot.springai.service.ChatService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@Validated
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(path = "/completion", produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(type = RateLimitType.USER, limit = 20, window = 60, message = "对话请求过于频繁，每分钟最多20次")
    public Object completion(@RequestParam @NotBlank String prompt) {
        return chatService.complete(prompt);
    }

    @GetMapping(path = "/health")
    public String health() {
        return "OK";
    }
}


