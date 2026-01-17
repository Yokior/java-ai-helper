package com.yokior.hook;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Content;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Yokior
 * @description
 * @date 2026/1/16 15:48
 */
@HookPositions({HookPosition.BEFORE_MODEL})
@Component
@Slf4j
public class MySummarizationHook extends MessagesModelHook {

    // 最大消息长度
    private static final int MAX_MSG_LENGTH = 20;

    // 保存最近消息数量 不能大于最大消息长度
    private static final int SAVE_MSG_LENGTH = 5;

    // 最大token数
    private static final long MAX_TOKEN_COUNT = 500;


    private static final String SUMMARY_PROMPT = """
            # Role
            AI聊天记录深度分析专家
                                    
            # Goal
            对提供的聊天记录进行语义层面的深度分析与重构，输出一份逻辑严密、信息密度高的核心摘要。
                                    
            # Constraints
            1. **零废话原则**：直接切入核心议题，严禁使用“对话提到”、“总结如下”、“记录显示”等元引导词。
            2. **内容降噪**：完全忽略寒暄、客套及与核心议题无关的噪音信息。
            3. **客观中立**：剔除主观情绪色彩，仅保留事实性描述、关键立场及逻辑推演。
            4. **字数控制**：严格控制在2000字以内。
            5. **结构化输出**：按“背景与核心议题 -> 关键冲突与论证逻辑 -> 最终结论/共识”的线性逻辑推进。
                                    
            # Workflow
            1. 识别对话背景与核心矛盾点。
            2. 提取各方关键论据及技术/业务细节。
            3. 归纳最终达成的技术方案、决策或待办事项。
                        """;

    private static final String SUMMARY_PREFIX = """
            【之前的聊天内容总结】
            """;


    private ChatClient chatClient;


    public MySummarizationHook(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }


    @Override
    public String getName() {
        return "MySummarizationHook";
    }


    @Override
    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {

        log.info("{} 消息列表长度：{}", config.threadId(), previousMessages.size());

        if (previousMessages.size() > MAX_MSG_LENGTH) {

            log.info("消息 > {}, {} 开始进行消息总结", MAX_MSG_LENGTH,config.threadId());

            // 获取总结内容

            String summarizeText = summarizeText(previousMessages);

            UserMessage summarizeUserMessage = UserMessage.builder()
                    .text(SUMMARY_PREFIX + summarizeText)
                    .build();

            // 获取系统提示词（如果有的话）
            SystemMessage systemMessage = null;
            int index = 0;
            for (Message message : previousMessages) {
                if (message instanceof SystemMessage) {
                    systemMessage = (SystemMessage) message;
                    log.info("在位置 {} 获取到系统提示词：{}", index, systemMessage.getText());
                    break;
                }
                index++;
            }

            // 保留最近的几条消息
            List<Message> newMessageList = new ArrayList<>(SAVE_MSG_LENGTH);

            List<Message> saveMessageList = previousMessages.subList(previousMessages.size() - SAVE_MSG_LENGTH, previousMessages.size());

            // 组装新消息列表
            Optional.ofNullable(systemMessage).ifPresent(newMessageList::add);
            newMessageList.add(summarizeUserMessage);
            newMessageList.addAll(saveMessageList);

            return new AgentCommand(newMessageList);
        }

        return new AgentCommand(previousMessages);
    }





    private String summarizeText(List<Message> messages) {

        for (Message message : messages) {
            log.info("{} 消息：{}", message.getMessageType().getValue(), message.getText());
        }

        messages.add(new UserMessage("请总结当前对话内容，并返回一个核心摘要。"));

        String content = chatClient.prompt()
                .system(SUMMARY_PROMPT)
                .messages(messages)
                .call()
                .content();

        messages.remove(messages.size() - 1);

//        log.info("获取的总结内容：{}", content);
        return content;
    }



    /**
     * 粗略估算Token数量
     * 规则参考：
     * 1. 英文字符：约4个字符 = 1 Token
     * 2. 中文字符：约1.5-2个字符 = 1 Token (中文更密集，通常按1.5估算)
     * 3. 空格/标点：通常视为1个字符参与计算
     *
     * @param text 输入文本
     * @return 估算的token数量
     */
    public static int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int length = text.length();
        int englishCharCount = 0;
        int nonEnglishCharCount = 0;

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            // 判断是否为ASCII字符（主要针对英文）
            if (c <= 127) {
                englishCharCount++;
            } else {
                nonEnglishCharCount++;
            }
        }

        // 英文按4字符=1token，中文/其他按1.5字符=1token
        double tokenCount = (englishCharCount / 4.0) + (nonEnglishCharCount / 1.5);

        return (int) Math.ceil(tokenCount);
    }

}
