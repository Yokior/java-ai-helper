package com.yokior.saver;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.serializer.Serializer;
import com.alibaba.cloud.ai.graph.serializer.check_point.CheckPointSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yokior.entity.ChatLog;
import com.yokior.entity.UserConversation;
import com.yokior.service.chatexpiration.IChatExpirationService;
import com.yokior.service.chatlog.IChatSaverService;
import com.yokior.service.userconversation.IUserConversationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Yokior
 * @description
 * @date 2026/1/12 16:22
 */
@Component
@Slf4j
public class MyRedisSaver implements BaseCheckpointSaver {


    // 默认TTL 不在set中使用 详情见 com.yokior.service.chatexpiration.ChatExpirationService
    public static final long TTL_SECONDS = 60 * 60 * 2;

    // 兜底用的TTL 默认时间是上述的2倍
    private static final long TTL_SECONDS_BACKUP = TimeUnit.SECONDS.toSeconds(TTL_SECONDS * 2);

    public static final String CONVERSATION_CONFIG_PREFIX = "chat:conservation:config:";
    public static final String CHECKPOINT_PREFIX = "chat:checkpoint:content:";
    public static final String LOCK_PREFIX = "chat:checkpoint:lock:";


    public static final String USER_ID = "user_id";


    private final Serializer<Checkpoint> checkpointSerializer;

    @Autowired
    private RedissonClient redisson;

    @Autowired
    private IChatSaverService chatSaverService;

    @Autowired
    private IUserConversationService userConversationService;

    @Autowired
    private IChatExpirationService chatExpirationService;


    public MyRedisSaver() {
        this.checkpointSerializer = new CheckPointSerializer(new SpringAIJacksonStateSerializer(OverAllState::new, new ObjectMapper()));
    }

    private String serializeCheckpoints(List<Checkpoint> checkpoints) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeInt(checkpoints.size());
            for (Checkpoint checkpoint : checkpoints) {
                checkpointSerializer.write(checkpoint, oos);
            }
            oos.flush();
            byte[] bytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(bytes);
        }
    }

    private LinkedList<Checkpoint> deserializeCheckpoints(String content) throws IOException, ClassNotFoundException {
        if (content == null || content.isEmpty()) {
            return new LinkedList<>();
        }
        byte[] bytes = Base64.getDecoder().decode(content);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            int size = ois.readInt();
            LinkedList<Checkpoint> checkpoints = new LinkedList<>();
            for (int i = 0; i < size; i++) {
                checkpoints.add(checkpointSerializer.read(ois));
            }
            return checkpoints;
        }
    }


    @Override
    public Collection<Checkpoint> list(RunnableConfig config) {
        String conversationId = getConversationIdByConfig(config);
        log.debug("{} 调用list", conversationId);

        RLock lock = redisson.getLock(LOCK_PREFIX + conversationId);
        try {

            boolean b = lock.tryLock(1, TimeUnit.SECONDS);
            if (!b) {
                return new LinkedList<>();
            }

            // 获取conversationId对应的RBucket
            RBucket<String> bucket = redisson.getBucket(CHECKPOINT_PREFIX + conversationId);
            String content = bucket.get();

            if (content == null) {
                return new LinkedList<>();
            }

            return deserializeCheckpoints(content);


        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public Optional<Checkpoint> get(RunnableConfig config) {
        // 获取线程名称
        String conversationId = getConversationIdByConfig(config);
        log.debug("{} 调用get", conversationId);


        RLock lock = redisson.getLock(LOCK_PREFIX + conversationId);
        try {

            boolean b = lock.tryLock(1, TimeUnit.SECONDS);
            if (!b) {
                return Optional.empty();
            }

            // 获取conversationId对应的RBucket
            RBucket<String> bucket = redisson.getBucket(CHECKPOINT_PREFIX + conversationId);
            String content = bucket.get();

            // 如果没有聊天记录说明是新对话 存储配置信息
            if (content == null) {
                String userId = getUserIdByConfig(config);
                RMap<String, String> map = redisson.getMap(CONVERSATION_CONFIG_PREFIX + conversationId);
                map.put(USER_ID, userId);
                map.expire(Duration.ofSeconds(TTL_SECONDS_BACKUP));

                return Optional.empty();
            }

            LinkedList<Checkpoint> checkpoints = deserializeCheckpoints(content);

            return getLast(checkpoints, config);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }


    }

    @Override
    public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
        // 获取线程名称
        String conversationId = getConversationIdByConfig(config);
        log.debug("{} 调用put", conversationId);

        RLock lock = redisson.getLock(LOCK_PREFIX + conversationId);
        try {

            boolean b = lock.tryLock(1, TimeUnit.SECONDS);
            if (!b) {
                return config;
            }

            // 获取conversationId对应的RBucket
            RBucket<String> bucket = redisson.getBucket(CHECKPOINT_PREFIX + conversationId);
            String content = bucket.get();
            // 如果是新的对话 投递到队列
            if (content == null) {
                chatExpirationService.post(conversationId);
            }
            LinkedList<Checkpoint> checkpoints = deserializeCheckpoints(content);


            // 将新的checkpoint加入队头 并保存bucket
            checkpoints.push(checkpoint);
            bucket.set(serializeCheckpoints(checkpoints), Duration.ofSeconds(TTL_SECONDS_BACKUP));

            return config;

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }


    }

    @Override
    public Tag release(RunnableConfig config) throws Exception {
        return null;
    }


    /**
     * 关闭会话
     * 将redis中存储的会话信息删除
     * 存储到数据库中备用
     *
     * @param conversationId
     */
    @Transactional
    public void closeConversation(String conversationId) {

        if (StringUtils.isEmpty(conversationId)) {
            throw new RuntimeException("conversationId 不能为空！");
        }

        RLock lock = redisson.getLock(LOCK_PREFIX + conversationId);
        try {

            boolean b = lock.tryLock(1, TimeUnit.SECONDS);
            if (!b) {
                return;
            }

            // 获取conversationId对应的RBucket
            RBucket<String> bucket = redisson.getBucket(CHECKPOINT_PREFIX + conversationId);
            RMap<String, String> map = redisson.getMap(CONVERSATION_CONFIG_PREFIX + conversationId);
            String content = bucket.get();
            if (content == null) {
                return;
            }
            String userId = map.get(USER_ID);
            if (userId == null) {
                return;
            }

            // 解析数据
            LinkedList<Checkpoint> checkpoints = deserializeCheckpoints(content);
            // 存储到数据库中
            // 先进行查询 如果存在则进行更新 如果不存在则进行插入
            ChatLog dbChatLog = chatSaverService.getById(conversationId);
            if (dbChatLog != null) {
                dbChatLog.setContent(checkpoints);
                dbChatLog.setUpdateTime(LocalDateTime.now());
                chatSaverService.updateById(dbChatLog);
            } else {
                // 不存在 则进行插入 需要插入2张表
                dbChatLog = new ChatLog();
                dbChatLog.setConversationId(conversationId);
                dbChatLog.setContent(checkpoints);
                dbChatLog.setCreateTime(LocalDateTime.now());
                dbChatLog.setUpdateTime(LocalDateTime.now());
                chatSaverService.save(dbChatLog);

                UserConversation userConversation = UserConversation.builder()
                        .conversationId(conversationId)
                        .userId(Long.valueOf(userId))
                        .build();
                userConversationService.save(userConversation);
            }

            // 删除redis中的数据
            bucket.delete();
            map.delete();


        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }


    }


    /**
     * 重新加载会话
     * 将数据库中存储的会话信息加载到redis中
     * 聊天记录以及配置信息
     * @param conversationId
     */
    public void reloadConversation(String conversationId) {

        if (StringUtils.isEmpty(conversationId)) {
            throw new RuntimeException("conversationId 不能为空！");
        }

        // 查询数据库对应的会话信息
        ChatLog dbChatLog = chatSaverService.getById(conversationId);
        if (dbChatLog == null) {
            throw new RuntimeException("对应的会话不存在！");
        }
        LinkedList<Checkpoint> checkpoints = dbChatLog.getContent();

        // 查询数据库对应的用户信息
        UserConversation userConversation = userConversationService.lambdaQuery()
                .eq(UserConversation::getConversationId, conversationId)
                .one();

        if (userConversation == null) {
            throw new RuntimeException("对应的用户信息不存在！");
        }
        Long userId = userConversation.getUserId();


        RLock lock = redisson.getLock(LOCK_PREFIX + conversationId);
        try {

            boolean b = lock.tryLock(1, TimeUnit.SECONDS);
            if (!b) {
                throw new RuntimeException("获取锁失败！");
            }

            // 获取conversationId对应的RBucket
            RBucket<String> bucket = redisson.getBucket(CHECKPOINT_PREFIX + conversationId);
            bucket.set(serializeCheckpoints(checkpoints), Duration.ofSeconds(TTL_SECONDS_BACKUP));
            // 获取conversationId对应的RMap
            RMap<String, String> map = redisson.getMap(CONVERSATION_CONFIG_PREFIX + conversationId);
            map.put(USER_ID, userId.toString());
            map.expire(Duration.ofSeconds(TTL_SECONDS_BACKUP));

            // 添加到过期处理延迟队列中
            chatExpirationService.post(conversationId);

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }



    /**
     * 获取线程名称
     * 从config中获取的threadId实际上就是conversationId
     *
     * @param config
     * @return
     */
    private String getConversationIdByConfig(RunnableConfig config) {
        Optional<String> conversationIdOpt = config.threadId();
        if (!conversationIdOpt.isPresent()) {
            throw new RuntimeException("threadId 不能为空！");
        }
        return conversationIdOpt.get();
    }


    private String getUserIdByConfig(RunnableConfig config) {
        Optional<Object> userId = config.metadata("userId");
        if (!userId.isPresent()) {
            throw new RuntimeException("userId 不能为空！");
        }
        return userId.get().toString();
    }


}
