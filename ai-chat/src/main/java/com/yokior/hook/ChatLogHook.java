package com.yokior.hook;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * @author Yokior
 * @description
 * @date 2026/1/11 20:41
 */
@HookPositions({HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
@Slf4j
public class ChatLogHook extends MessagesModelHook {
    @Override
    public String getName() {
        return "聊天消息日志HOOK";
    }

    @Override
    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {

        log.info("请求AI：{}", previousMessages.get(previousMessages.size() - 1));

        return super.beforeModel(previousMessages, config);
    }

    @Override
    public AgentCommand afterModel(List<Message> previousMessages, RunnableConfig config) {

        log.info("AI响应：{}", previousMessages.get(previousMessages.size() - 1));

        return super.afterModel(previousMessages, config);
    }
}
