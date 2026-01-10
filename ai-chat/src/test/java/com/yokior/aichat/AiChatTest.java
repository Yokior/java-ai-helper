package com.yokior.aichat;

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
        String res = aiChatService.test("不同团队之间数据隔离是如何实现的？");
        log.info(res);
    }
}
