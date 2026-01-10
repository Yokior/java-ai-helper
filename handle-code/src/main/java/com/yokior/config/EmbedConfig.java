package com.yokior.config;

import lombok.Data;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author Yokior
 * @description
 *
 * 因为springai ollama的embedding模型不支持dimensions参数
 * 权宜之计：使用ollama的embedding模型替换openai的embedding模型
 * PS：已经给SpringAI官方提交PR #5176
 *
 * @date 2026/1/11 0:10
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ollama")
public class EmbedConfig {

    private String baseUrl;

    private String apiKey;

    private String model;

    private Integer dimensions;

    @Bean
    @Primary
    public EmbeddingModel ollama2OpenAiEmbeddingModel(OpenAiApi openAiApi) {

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(model)
                .dimensions(dimensions)
                .build();

        OpenAiApi api = openAiApi.mutate().baseUrl(baseUrl).apiKey(apiKey).build();

        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options);
    }

}
