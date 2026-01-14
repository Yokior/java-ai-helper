package com.yokior.chatlog;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.yokior.AiChatStarter;
import com.yokior.entity.ChatLog;
import com.yokior.saver.MyRedisSaver;
import com.yokior.service.chatlog.IChatSaverService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.LinkedList;

/**
 * @author Yokior
 * @description
 * @date 2026/1/13 17:42
 */
@SpringBootTest(classes = {AiChatStarter.class})
@Slf4j
public class ChatLogTest {

    @Autowired
    private IChatSaverService chatSaverService;

    @Autowired
    private MyRedisSaver myRedisSaver;

    @Test
    void testInsert() {

        String threadId = "test-thread";
        LinkedList<Checkpoint> list = (LinkedList<Checkpoint>) myRedisSaver.list(RunnableConfig.builder().threadId(threadId).build());

        ChatLog chatlog = ChatLog.builder()
                .conversationId("123456")
                .content(list)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        chatSaverService.save(chatlog);
    }

    @Test
    void testQuery() {

        ChatLog chatlog = chatSaverService.getById("123456");

        log.info("{}", chatlog.getContent());
    }



    @Test
    void testCloseConversation() {
        String conversationId = "test-conversation-id-001";
        myRedisSaver.closeConversation(conversationId);
    }


    @Test
    void testReloadConversation() {
        String conversationId = "test-conversation-id-001";
        myRedisSaver.reloadConversation(conversationId);
    }

}
