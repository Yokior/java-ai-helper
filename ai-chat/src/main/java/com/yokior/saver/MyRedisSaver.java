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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
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

    private static final String THREAD_META_PREFIX = "graph:thread:meta:";
    private static final String CHECKPOINT_PREFIX = "graph:checkpoint:content:";
    private static final String LOCK_PREFIX = "graph:checkpoint:lock:";
    private static final String THREAD_REVERSE_PREFIX = "graph:thread:reverse:";

    private static final String FIELD_THREAD_ID = "thread_id";
    private static final String FIELD_THREAD_NAME = "thread_name";

    private final Serializer<Checkpoint> checkpointSerializer;
    private RedissonClient redisson;

    public MyRedisSaver(RedissonClient redisson) {
        this.redisson = redisson;
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
        String threadName = getThreadNameByConfig(config);
        log.info("{} 调用list", threadName);

        RLock lock = redisson.getLock(LOCK_PREFIX + threadName);
        try {

            boolean b = lock.tryLock(1, TimeUnit.SECONDS);
            if (!b) {
                return new LinkedList<>();
            }

            // 获取线程id
            String threadId = getThreadIdByThreadName(threadName);
            if (threadId == null) {
                return new LinkedList<>();
            }

            // 获取线程id对应的RBucket
            RBucket<String> bucket = redisson.getBucket(CHECKPOINT_PREFIX + threadId);
            String content = bucket.get();
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
        String threadName = getThreadNameByConfig(config);
        log.info("{} 调用get", threadName);


        RLock lock = redisson.getLock(LOCK_PREFIX + threadName);
        try {

            boolean b = lock.tryLock(1, TimeUnit.SECONDS);
            if (!b) {
                return Optional.empty();
            }

            // 从redis中获取线程对应id
            String threadId = getThreadIdByThreadName(threadName);
            if (threadId == null) {
                return Optional.empty();
            }

            // 获取线程id对应的RBucket
            RBucket<String> bucket = redisson.getBucket(CHECKPOINT_PREFIX + threadId);
            String content = bucket.get();
            LinkedList<Checkpoint> checkpoints = null;
            checkpoints = deserializeCheckpoints(content);

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
        String threadName = getThreadNameByConfig(config);
        log.info("{} 调用put", threadName);

        RLock lock = redisson.getLock(LOCK_PREFIX + threadName);
        try {

            boolean b = lock.tryLock(1, TimeUnit.SECONDS);
            if (!b) {
                return config;
            }

            String threadId = getThreadIdByThreadNameOrCreate(threadName);

            // 获取线程id对应的RBucket
            RBucket<String> bucket = redisson.getBucket(CHECKPOINT_PREFIX + threadId);
            String content = bucket.get();
            LinkedList<Checkpoint> checkpoints = deserializeCheckpoints(content);

            // 将新的checkpoint加入队头 并保存bucket
            checkpoints.push(checkpoint);
            bucket.set(serializeCheckpoints(checkpoints));

            return config;

        }
        finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }


    }

    @Override
    public Tag release(RunnableConfig config) throws Exception {
        log.info("MyRedisSaver - 调用release");
        return null;
    }


    /**
     * 直接获取线程id 可能为空
     * @param threadName
     * @return
     */
    private String getThreadIdByThreadName(String threadName) {
        RMap<String, String> map = redisson.getMap(THREAD_META_PREFIX + threadName);

        String threadId = map.get(FIELD_THREAD_ID);
        return threadId;
    }



    /**
     * 获取线程id 如果不存在则创建
     * @param threadName
     * @return
     */
    private String getThreadIdByThreadNameOrCreate(String threadName) {
        RMap<String, String> map = redisson.getMap(THREAD_META_PREFIX + threadName);

        String threadId = map.get(FIELD_THREAD_ID);

        if (threadId == null) {
            threadId = UUID.randomUUID().toString();
            map.put(FIELD_THREAD_ID, threadId);

            // 创建反向索引
            RMap<String, String> rMap = redisson.getMap(THREAD_REVERSE_PREFIX + threadId);
            rMap.put(FIELD_THREAD_NAME, threadName);
        }

        return threadId;
    }


    /**
     * 获取线程名称
     * 从config中获取的threadId实际上就是threadName
     * @param config
     * @return
     */
    private String getThreadNameByConfig(RunnableConfig config) {
        Optional<String> threadNameOpt = config.threadId();
        if (!threadNameOpt.isPresent()) {
            throw new RuntimeException("threadId 不能为空！");
        }
        return threadNameOpt.get();
    }


}
