package com.yokior.service.chatexpiration;

import com.yokior.saver.MyRedisSaver;
import com.yokior.service.chatlog.IChatSaverService;
import com.yokior.service.userconversation.IUserConversationService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @author Yokior
 * @description
 * @date 2026/1/15 16:18
 */
@Service
@Slf4j
public class ChatExpirationService implements IChatExpirationService {

    @Autowired
    private RedissonClient redisson;

    @Autowired
    @Qualifier("handleExpireConversation")
    private Executor handleExpireConversationExecutor;

    @Autowired
    @Lazy
    private MyRedisSaver myRedisSaver;

    private static final String CHAT_EXPIRATION_QUEUE = "chat:expiration:queue";

    @PostConstruct
    public void start() {
        handleExpireConversationExecutor.execute(() -> {
            RBlockingQueue<String> queue = redisson.getBlockingQueue(CHAT_EXPIRATION_QUEUE);

            log.info("handleExpireConversation监听线程已启动");

            while (true) {
                try {
                    // 阻塞等待 直到获取到数据
                    String conversationId = queue.take();

                    myRedisSaver.closeConversation(conversationId);
                    log.info("会话 {} 已过期, 自动关闭", conversationId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }

            }
        });
    }



    @Override
    public void post(String conversationId) {
        RQueue<String> queue = redisson.getQueue(CHAT_EXPIRATION_QUEUE);
        RDelayedQueue<String> delayedQueue = redisson.getDelayedQueue(queue);

        // 添加到延迟队列中
        delayedQueue.offer(conversationId, MyRedisSaver.TTL_SECONDS, TimeUnit.SECONDS);
    }
}
