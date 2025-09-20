package cn.iocoder.boot.springai.config;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIConfig {

    @Bean
    public OpenAiApi openAiApi(
            @Value("${spring.ai.openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${spring.ai.openai.api-key:}") String apiKey
    ) {
        return new OpenAiApi(baseUrl, apiKey);
    }

    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi,
                                           @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String model) {
        return new OpenAiChatModel(openAiApi, model);
    }

    @Bean
    public OpenAiEmbeddingModel openAiEmbeddingModel(OpenAiApi openAiApi,
                                                     @Value("${spring.ai.openai.embedding.options.model:text-embedding-3-small}") String model) {
        return new OpenAiEmbeddingModel(openAiApi, model);
    }
}


