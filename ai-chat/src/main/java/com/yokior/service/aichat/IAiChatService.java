package com.yokior.service.aichat;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.Optional;

/**
 * @author Yokior
 * @description
 * @date 2026/1/10 22:23
 */
public interface IAiChatService {

    String test(String userQuery);

    AssistantMessage agentTest(String userQuery) throws GraphRunnerException;

    Optional<NodeOutput> agentTestHuman(String userQuery) throws GraphRunnerException;
}
