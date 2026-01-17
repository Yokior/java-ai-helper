package com.yokior.controller;

import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.yokior.saver.MyRedisSaver;
import com.yokior.service.aichat.IAiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Yokior
 * @description
 * @date 2026/1/15 17:37
 */
@RestController
public class TestController {

    @Autowired
    private MyRedisSaver myRedisSaver;

    @Autowired
    private IAiChatService aiChatService;


    /**
     * 测试延迟队列
     */
    @GetMapping("/testQueue")
    public String testQueue() {

//        myRedisSaver.reloadConversation("test-conversation-id-001");

        return "testQueue";
    }


    @GetMapping("/testAgentOnce")
    public String testAgentOnce(String userQuery) throws GraphRunnerException {
        return aiChatService.agentOnce(userQuery);
    }

}
