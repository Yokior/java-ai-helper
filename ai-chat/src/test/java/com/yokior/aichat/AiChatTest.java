package com.yokior.aichat;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.yokior.AiChatStarter;
import com.yokior.saver.MyRedisSaver;
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

    @Autowired
    private MyRedisSaver myRedisSaver;


    @Test
    void test() {
        String res = aiChatService.test("怎么创建团队？");
        log.info(res);
    }

    @Test
    void agentTest() throws GraphRunnerException {
        String res = aiChatService.agentTest("现在是几点钟");
        log.info(res);
    }

    @Test
    void listCheckpoint() {
        String threadId = "test-thread";
        log.info("{}", JSONObject.toJSONString(myRedisSaver.list(RunnableConfig.builder().threadId(threadId).build())));
    }
}
