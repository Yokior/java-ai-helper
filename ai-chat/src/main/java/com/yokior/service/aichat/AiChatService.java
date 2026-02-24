package com.yokior.service.aichat;


import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.postgresql.PostgresSaver;
import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.yokior.advisor.ChatLogAdvisor;
import com.yokior.common.EmbedSearchResult;
import com.yokior.entity.ChatStreamResponse;
import com.yokior.hook.ChatLogHook;
import com.yokior.saver.MyRedisSaver;
import com.yokior.service.embedding.IEmbeddingService;
import com.yokior.service.milvus.IMilvusService;
import com.yokior.tool.DateTimeTools;
import com.yokior.tool.MilvusSearchTool;
import com.yokior.tool.SearchTool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * @author Yokior
 * @description
 * @date 2026/1/10 22:40
 */
@Service
@Slf4j
public class AiChatService implements IAiChatService {

    private ChatClient chatClient;

    @Autowired
    private IMilvusService milvusService;

    @Autowired
    private IEmbeddingService embeddingService;

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private MyRedisSaver myRedisSaver;

    @Autowired
    private MilvusSearchTool milvusSearchTool;


    private static final String AI_NAME = "Java 项目专属技术助手";

    private static final String SYSTEM_PROMPT = """
            # Java 项目专属技术助手行为规范
                
            ## 🎯 角色定位
            你是一名特定 Java 项目的**专属技术助手**。你的核心能力是将用户的自然语言提问精准映射到项目代码库中，基于**真实的代码历史**提供技术支持。你不仅是一个问答机器人，更是该项目的**“代码记忆体”**与**“技术解读员”**。
                     
            > ⚠️ **核心约束**：你访问的向量数据库中**仅包含该项目**的代码片段及上下文。严禁编造不存在的代码，严禁脱离项目实际进行泛泛而谈。
                     
            ---
                     
            ## 🛑 核心执行准则 (最高优先级)
            1. **必须检索**：在回答用户提出的任何技术问题、代码逻辑或功能查询之前，**必须**先调用向量数据库检索工具（`检索向量数据库`）。**严禁在未检索的情况下直接回答、猜测或拒绝。**
            2. **工具优先**：你的第一反应应该是“如何构建查询语句去调用工具”，而不是“我是否知道答案”。
            3. **如实反馈**：答案必须严格基于工具返回的检索结果。若检索结果为空或无法回答，请如实告知，**不得编造**。
                     
            ---
                     
            ## 📋 标准处理流程
                     
            ### 1. 理解意图与触发检索
            *   分析用户提问，识别技术关键词、类名、方法名或业务场景。
            *   **立即调用** `检索向量数据库` 工具。
                *   如果是模糊提问（如“怎么处理订单”），优化关键词为“OrderService”、“processOrder”后再检索。
                *   如果是具体代码问题，直接使用完整类名或方法路径检索。
            *   *注意：只有获得了工具返回的检索结果后，方可进入下一步。*
                     
            ### 2. 评估检索结果
            获取工具返回结果后，评估其相关性：
            *   **高度相关**：结果包含用户查询的类、方法或具体逻辑块。
            *   **部分相关**：结果包含相关模块但缺少具体细节，或包含模糊的上下文。
            *   **完全不相关/无结果**：工具返回为空，或结果与当前项目无关。
                     
            ### 3. 策略调整与重试 (仅限结果不理想时)
            若评估结果为“部分相关”或“完全不相关”，请执行重试策略（最多重试 2 次）：
            *   **更换关键词**：尝试同义词（如将“插入”改为“save”、“insert”）、缩写或全称。
            *   **细化上下文**：添加包名（如 `com.service`）、注释标记或特定异常名作为过滤条件。
            *   **拆解问题**：若问题复杂，将其拆分为多个子问题分别检索。
                     
            ### 4. 生成最终回答
            基于（经过重试后的）最佳检索结果生成回答：
            *   **有结果**：结合 Java 专业知识，解释代码逻辑、引用代码片段或提供调用路径。
            *   **仍无结果**：告知用户未找到实现，**严禁推测**。
                     
            ---
                     
            ## ✅ 作答原则与规范
                     
            *   **真实性优先**：引用代码必须真实存在于项目中。不要说“通常做法是...”，要说“在 `XxxService.java` 中，代码实现如下...”。
            *   **精准性导向**：避免通用的 Java 教程式回答，必须聚焦项目中的具体实现。
            *   **用户友好**：使用开发者易于理解的技术语言。解释“为什么代码要这样写”，而不仅仅是“代码写了什么”。
                     
            ---
                     
            ## ⚠️ 异常情况处理 (仅在检索后使用)
                     
            **只有在你已经调用了检索工具，且确认项目代码中确实不存在相关实现时，方可使用以下话术：**
                     
            *   **项目中无相关实现**
                > ❌ “在当前项目代码库中，**未找到**关于 [功能/模块] 的相关实现。”
                     
            *   **现有代码无法确定行为**
                > ❓ “根据现有代码片段，**无法确定** [功能] 的具体行为逻辑，建议检查配置文件或外部依赖。”
                     
            *   **多种可能性/不确定**
                > ⚠️ “该项目中可能存在多种实现方式，或代码逻辑较为分散，建议重点检查 `com.example.xxx` 模块下的相关类。”
                     
            ---
                     
            ## 💡 最后检查
            在输出任何回答之前，请自问：
            1. 我是否已经调用了检索工具？
            2. 我的回答是否完全基于工具返回的代码片段？
            3. 我是否避免了编造不存在的代码？
                  """;


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
    public AssistantMessage agentTest(String userQuery) throws GraphRunnerException {

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


        ReactAgent agent = ReactAgent.builder()
                .name("助手")
                .systemPrompt("你是一个助手")
                .model(chatModel)
                .tools(searchToolCallback)
                .methodTools(new DateTimeTools())
//                .interceptors(new ChatLogInterceptor()) // 记录完整API调用
                .hooks(new ChatLogHook()) // 记录聊天记录
                .saver(myRedisSaver)
                .build();

        RunnableConfig config = RunnableConfig.builder()
                .threadId("test-conversation-id-001")
                .addMetadata("userId", "666666")
                .build();

        return agent.call(userQuery, config);
    }

    @Override
    public String chatOnce(String userQuery) {

        return chatClient.prompt()
                .user(userQuery)
                .call()
                .content();
    }

    @Override
    public String agentOnce(String userQuery) throws GraphRunnerException {
        ReactAgent agent = ReactAgent.builder()
                .name(AI_NAME)
                .systemPrompt(SYSTEM_PROMPT)
                .model(chatModel)
                .methodTools(milvusSearchTool)
                .hooks(new ChatLogHook())
                .build();

        return agent.call(userQuery).getText();
    }

    @Override
    public SseEmitter agentOnceStream(String userQuery) throws GraphRunnerException {

        SseEmitter sseEmitter = new SseEmitter();

        ReactAgent agent = ReactAgent.builder()
                .name(AI_NAME)
                .systemPrompt(SYSTEM_PROMPT)
                .model(chatModel)
                .methodTools(milvusSearchTool)
                .hooks(new ChatLogHook())
                .build();

        Flux<NodeOutput> stream = agent.stream(userQuery);

        stream.subscribe(
                output -> {
                    // 检查是否为 StreamingOutput 类型
                    if (output instanceof StreamingOutput streamingOutput) {
                        OutputType type = streamingOutput.getOutputType();

                        // 处理模型推理的流式输出
                        if (type == OutputType.AGENT_MODEL_STREAMING) {
                            String text = streamingOutput.message().getText();
                            // 流式增量内容，逐步显示
                            if (StringUtils.isNotBlank(text)) {
                                sendStream(sseEmitter, text);
                            }

                        } else if (type == OutputType.AGENT_MODEL_FINISHED) {
                            // 模型推理完成，可获取完整响应
                            log.info("\n模型输出完成");
                        }

                        // 处理工具调用完成（目前不支持 STREAMING）
                        if (type == OutputType.AGENT_TOOL_FINISHED) {
                            log.info("工具调用完成: " + output.node());
                        }

                        // 对于 Hook 节点，通常只关注完成事件（如果Hook没有有效输出可以忽略）
                        if (type == OutputType.AGENT_HOOK_FINISHED) {
                            log.info("Hook 执行完成: " + output.node());
                        }
                    }
                },
                error -> log.error("错误: " + error),
                () -> {
                    sseEmitter.complete();
                }
        );

        return sseEmitter;
    }


    /**
     * 发送流式数据
     *
     * @param sseEmitter
     * @param text
     */
    private void sendStream(SseEmitter sseEmitter, String text) {
        try {
            ChatStreamResponse response = ChatStreamResponse.builder()
                    .content(text)
                    .build();

            sseEmitter.send(SseEmitter.event().data(response));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
