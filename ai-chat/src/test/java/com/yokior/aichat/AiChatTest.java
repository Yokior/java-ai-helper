package com.yokior.aichat;

import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.yokior.AiChatStarter;
import com.yokior.service.aichat.IAiChatService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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


    @Test
    void test() {
        String res = aiChatService.test("怎么创建团队？");
        log.info(res);
    }

    @Test
    void agentTest() throws GraphRunnerException {
        String res = aiChatService.agentTest("告诉我现在的时间");
        log.info(res);
    }
}
