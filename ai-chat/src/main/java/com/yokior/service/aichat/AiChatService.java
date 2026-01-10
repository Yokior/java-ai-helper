package com.yokior.service.aichat;

import com.yokior.advisor.ChatLogAdvisor;
import com.yokior.common.EmbedSearchResult;
import com.yokior.service.embedding.IEmbeddingService;
import com.yokior.service.milvus.IMilvusService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Yokior
 * @description
 * @date 2026/1/10 22:40
 */
@Service
public class AiChatService implements IAiChatService {

    private ChatClient chatClient;

    @Autowired
    private IMilvusService milvusService;

    @Autowired
    private IEmbeddingService embeddingService;


    public AiChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(new ChatLogAdvisor())
                .build();
    }

    @Override
    public String test(String userQuery) {
        String embedQuery = chatClient.prompt()
                .system("""
                           你是一个提示词优化专家，现在用户对代码项目进行了问题提问，你需要将提问优化成便于向量化检索的内容，直接输出优化后的内容                     
                        """)
                .user(userQuery)
                .call()
                .content();

        List<Float> vector = embeddingService.embedding(embedQuery);
        StringBuilder ask = new StringBuilder();
        ask.append("你是一个Java代码专家，用户进行了提问，下面是根据问题检索向量数据库得到的格式化代码片段知识库，根据知识库问题进行回答，切记不可随意编造！如实回答！").append("\n");
        for (EmbedSearchResult result : milvusService.search(vector, 5)) {
            ask.append("所属类: ").append(result.getClassName()).append("\n");
            ask.append("内容:").append(result.getContent()).append("\n");
        }

        return chatClient.prompt()
                .system(ask.toString())
                .user(userQuery)
                .call()
                .content();
    }
}
