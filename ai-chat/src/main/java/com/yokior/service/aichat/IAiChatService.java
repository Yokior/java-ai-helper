package com.yokior.service.aichat;

import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;

/**
 * @author Yokior
 * @description
 * @date 2026/1/10 22:23
 */
public interface IAiChatService {

    String test(String userQuery);

    String agentTest(String userQuery) throws GraphRunnerException;
}
