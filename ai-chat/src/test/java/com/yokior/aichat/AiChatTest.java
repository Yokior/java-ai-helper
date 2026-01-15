package com.yokior.aichat;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.yokior.AiChatStarter;
import com.yokior.hook.ChatLogHook;
import com.yokior.saver.MyRedisSaver;
import com.yokior.service.aichat.IAiChatService;
import com.yokior.tool.DateTimeTools;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Yokior
 * @description
 * @date 2026/1/10 22:40
 */
@SpringBootTest(classes = {AiChatStarter.class})
@Slf4j
public class AiChatTest {

    @Autowired
    private IAiChatService aiChatService;

    @Autowired
    private MyRedisSaver myRedisSaver;

    @Autowired
    private ChatModel chatModel;

    @Test
    void test() {
        String res = aiChatService.test("怎么创建团队？");
        log.info(res);
    }

    @Test
    void agentTest() throws GraphRunnerException {
        AssistantMessage res = aiChatService.agentTest("现在是几点钟");
        log.info(res.getText());
    }

    @Test
    void listCheckpoint() {
        String threadId = "test-thread";
        log.info("{}", JSONObject.toJSONString(myRedisSaver.list(RunnableConfig.builder().threadId(threadId).build())));
    }

    @Test
    void testHumanInTheLoop() throws GraphRunnerException {

        CountDownLatch latch = new CountDownLatch(1);

        String userQuery = "现在几点钟";


        HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder()
                .approvalOn("getCurrentDateTime", "需要手动确定getCurrentDateTime调用")
                .build();


        ReactAgent agent = ReactAgent.builder()
                .name("助手")
                .systemPrompt("你是一个助手")
                .model(chatModel)
                .methodTools(new DateTimeTools())
//                .interceptors(new ChatLogInterceptor()) // 记录完整API调用
                .hooks(humanInTheLoopHook)
                .hooks(new ChatLogHook()) // 记录聊天记录
                .saver(myRedisSaver)
                .build();

        RunnableConfig config = RunnableConfig.builder()
                .threadId("test-conversation-id-002")
                .addMetadata("userId", "666666")
                .build();

        Optional<NodeOutput> result = agent.invokeAndGetOutput(userQuery, config);

        if (result.isPresent() && result.get() instanceof InterruptionMetadata) {
            InterruptionMetadata interruptionMetadata = (InterruptionMetadata) result.get();

            System.out.println("检测到中断，需要人工审批");
            List<InterruptionMetadata.ToolFeedback> toolFeedbacks =
                    interruptionMetadata.toolFeedbacks();

            for (InterruptionMetadata.ToolFeedback feedback : toolFeedbacks) {
                System.out.println("工具: " + feedback.getName());
                System.out.println("参数: " + feedback.getArguments());
                System.out.println("描述: " + feedback.getDescription());
            }

            // 6. 模拟人工决策（这里选择批准）
            InterruptionMetadata.Builder feedbackBuilder = InterruptionMetadata.builder()
                    .nodeId(interruptionMetadata.node())
                    .state(interruptionMetadata.state());

            toolFeedbacks.forEach(toolFeedback -> {
                InterruptionMetadata.ToolFeedback approvedFeedback =
                        InterruptionMetadata.ToolFeedback.builder(toolFeedback)
                                .result(InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED)
                                .build();
                feedbackBuilder.addToolFeedback(approvedFeedback);
            });

            InterruptionMetadata approvalMetadata = feedbackBuilder.build();

            // 7. 第二次调用 - 使用人工反馈恢复执行
            System.out.println("批准");
                    RunnableConfig resumeConfig = RunnableConfig.builder()
                            .threadId("test-conversation-id-002")
                            .addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, approvalMetadata)
                            .build();

            Optional<NodeOutput> finalResult = agent.invokeAndGetOutput("", resumeConfig);


            if (finalResult.isPresent()) {
                System.out.println("执行完成");
                System.out.println("最终结果: " + finalResult.get());
            }

            try {
                latch.await(1, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }


    }

    @Test
    void waitTime() {
        CountDownLatch latch = new CountDownLatch(1);

        try {
            latch.await(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
