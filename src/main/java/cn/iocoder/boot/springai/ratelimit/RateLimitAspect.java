package cn.iocoder.boot.springai.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * 限流切面
 * 拦截带有@RateLimit注解的方法，执行限流检查
 */
@Aspect
@Component
public class RateLimitAspect {

    private final RateLimitService rateLimitService;
    private final ExpressionParser parser = new SpelExpressionParser();

    public RateLimitAspect(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        if (!rateLimit.enabled()) {
            return joinPoint.proceed();
        }

        String key = buildRateLimitKey(joinPoint, rateLimit);
        
        RateLimitService.RateLimitResult result = rateLimitService.isAllowed(
            key, 
            rateLimit.limit(), 
            rateLimit.window()
        );

        if (!result.isAllowed()) {
            // 限流被触发，返回错误响应
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("X-RateLimit-Limit", String.valueOf(rateLimit.limit()))
                .header("X-RateLimit-Window", String.valueOf(rateLimit.window()))
                .header("X-RateLimit-Remaining", String.valueOf(result.getRemaining()))
                .body(Map.of(
                    "success", false,
                    "error", "RATE_LIMIT_EXCEEDED",
                    "message", rateLimit.message(),
                    "limit", rateLimit.limit(),
                    "window", rateLimit.window(),
                    "remaining", result.getRemaining()
                ));
        }

        // 在响应头中添加限流信息
        Object response = joinPoint.proceed();
        
        if (response instanceof ResponseEntity<?> responseEntity) {
            return ResponseEntity.status(responseEntity.getStatusCode())
                .headers(responseEntity.getHeaders())
                .header("X-RateLimit-Limit", String.valueOf(rateLimit.limit()))
                .header("X-RateLimit-Window", String.valueOf(rateLimit.window()))
                .header("X-RateLimit-Remaining", String.valueOf(result.getRemaining()))
                .body(responseEntity.getBody());
        }

        return response;
    }

    /**
     * 构建限流key
     */
    private String buildRateLimitKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String baseKey = joinPoint.getSignature().toShortString();
        
        switch (rateLimit.type()) {
            case USER:
                return "user:" + getUserKey() + ":" + baseKey;
            case IP:
                return "ip:" + getClientIp() + ":" + baseKey;
            case API:
                return "api:" + baseKey;
            case GLOBAL:
                return "global:" + baseKey;
            default:
                return baseKey;
        }
    }

    /**
     * 获取用户key
     */
    private String getUserKey() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName();
        }
        return "anonymous";
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            
            // 检查代理IP
            String ip = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For可能包含多个IP，取第一个
                return ip.split(",")[0].trim();
            }
            
            ip = request.getHeader("X-Real-IP");
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
            
            return request.getRemoteAddr();
        }
        
        return "unknown";
    }

    /**
     * 解析SpEL表达式
     */
    private String parseExpression(String expressionString, ProceedingJoinPoint joinPoint) {
        if (!StringUtils.hasText(expressionString)) {
            return "";
        }

        try {
            Expression expression = parser.parseExpression(expressionString);
            EvaluationContext context = new StandardEvaluationContext();
            
            // 设置方法参数
            Object[] args = joinPoint.getArgs();
            for (int i = 0; i < args.length; i++) {
                context.setVariable("arg" + i, args[i]);
            }
            
            // 设置请求和认证对象
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                context.setVariable("request", attributes.getRequest());
            }
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                context.setVariable("authentication", authentication);
            }
            
            Object result = expression.getValue(context);
            return result != null ? result.toString() : "";
            
        } catch (Exception e) {
            System.err.println("解析SpEL表达式失败: " + expressionString + ", " + e.getMessage());
            return "";
        }
    }
}
