package com.yokior.service.aichat;



import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.postgresql.PostgresSaver;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.yokior.advisor.ChatLogAdvisor;
import com.yokior.common.EmbedSearchResult;
import com.yokior.hook.ChatLogHook;
import com.yokior.interceptor.ChatLogInterceptor;
import com.yokior.saver.MyRedisSaver;
import com.yokior.service.embedding.IEmbeddingService;
import com.yokior.service.milvus.IMilvusService;
import com.yokior.tool.DateTimeTools;
import com.yokior.tool.SearchTool;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private MyRedisSaver myRedisSaver;


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

    @Override
    public String agentTest(String userQuery) throws GraphRunnerException {

        FunctionToolCallback<String, String> searchToolCallback = FunctionToolCallback.builder("搜索工具", new SearchTool())
                .description("根据用户问题，从知识库中搜索内容")
                .inputType(String.class)
                .build();

        PostgresSaver postgresSaver = PostgresSaver.builder()
//                .createTables(true)
                .host("127.0.0.1")
                .port(5432)
                .user("postgres")
                .password("root")
                .database("my_test_database")
                .build();

        RedisSaver redisSaver = RedisSaver.builder()
                .redisson(redissonClient)
                .build();

        ReactAgent agent = ReactAgent.builder()
                .name("助手")
                .systemPrompt("你是一个助手")
                .model(chatModel)
                .tools(searchToolCallback)
                .methodTools(new DateTimeTools())
//                .interceptors(new ChatLogInterceptor())
                .hooks(new ChatLogHook())
                .saver(myRedisSaver)
                .build();

        RunnableConfig config = RunnableConfig.builder()
                .threadId("test-thread")
                .build();

        AssistantMessage message = agent.call(userQuery, config);

        return message.getText();
    }
}
