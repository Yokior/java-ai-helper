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

    /**
     * 仅供测试
     *
     * @param userQuery
     * @return
     */
    String test(String userQuery);

    /**
     * 仅供测试
     *
     * @param userQuery
     * @return
     */
    AssistantMessage agentTest(String userQuery) throws GraphRunnerException;

    /**
     * 一问一答 无记忆
     *
     * @param userQuery
     * @return
     */
    String chatOnce(String userQuery);


    /**
     * 一次性agent 无记忆
     *
     * @param userQuery
     * @return
     */
    String agentOnce(String userQuery) throws GraphRunnerException;

}
