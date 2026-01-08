package com.yokior.service.handlecode;

import com.yokior.common.SplitChunk;
import com.yokior.service.embedding.IEmbeddingService;
import com.yokior.service.milvus.IMilvusService;
import com.yokior.service.split.ISplitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author Yokior
 * @description
 * @date 2026/1/8 16:49
 */
@Service
@Slf4j
public class HandleCodeServiceImpl implements IHandleCodeService {

    @Autowired
    private ISplitService splitService;

    @Autowired
    private IEmbeddingService embeddingService;

    @Autowired
    private IMilvusService milvusService;

    @Autowired
    @Qualifier("codeProcessExecutor")
    private Executor codeProcessExecutor;

    @Autowired
    @Qualifier("insertExecutor")
    private Executor insertExecutor;

    // 使用阻塞队列作为生产者和消费者的缓冲区
    // LinkedBlockingQueue 是线程安全的，且自带阻塞功能，非常适合这里
    private final BlockingQueue<SplitChunk> chunkQueue = new LinkedBlockingQueue<>(1000);


    @Override
    public void splitAndEmbedAndSave(String projectPath) throws Exception {

        // 项目名取最后一级目录
        String projectName = Paths.get(projectPath).getFileName().toString();

        // 收集所有Java文件
        List<Path> javaFiles = Files.walk(Paths.get(projectPath))
                .filter(path -> path.toString().endsWith(".java"))
                .collect(Collectors.toList());

        List<SplitChunk> batchChunkList = new ArrayList<>(1000);
        int count = 0;
        // 记录开始时间
        long startTime = System.currentTimeMillis();

        for (Path path : javaFiles) {

            // 加载并分块
            List<SplitChunk> chunkList = splitService.loadAndSplit(path, projectName);
            log.debug("加载并分块 {} 耗时 {}s ", path.getFileName(), (System.currentTimeMillis() - startTime)/1000.0);

            // 遍历每一个分块
            for (SplitChunk chunk : chunkList) {
                // 向量化
                List<Float> vector = embeddingService.embedding(chunk.getContent());
                chunk.setVector(vector);
                count++;
            }
            log.debug("向量化 {} 块耗时 {}s", chunkList.size(), (System.currentTimeMillis() - startTime)/1000.0);

            batchChunkList.addAll(chunkList);

            // 每1000个分块向量保存一次数据库
            if (count >= 1000) {
                count = 0;
                milvusService.batchInsert(batchChunkList);
                log.debug("向数据库保存 {} 块耗时 {}s", batchChunkList.size(), (System.currentTimeMillis() - startTime)/1000.0);
                batchChunkList.clear();
            }
        }

        if (count > 0) {
            milvusService.batchInsert(batchChunkList);
            log.debug("向数据库保存 {} 块耗时 {}s", batchChunkList.size(), (System.currentTimeMillis() - startTime)/1000.0);
        }

    }


    @Override
    public void splitAndEmbedAndSaveAsync(String projectPath) throws Exception {

        String projectName = Paths.get(projectPath).getFileName().toString();

        // 1. 收集所有Java文件
        List<Path> javaFiles = Files.walk(Paths.get(projectPath))
                .filter(path -> path.toString().endsWith(".java"))
                .collect(Collectors.toList());

        long startTime = System.currentTimeMillis();

        // 停止信号
        SplitChunk stopSplitChunk = new SplitChunk();

        // --- 消费者线程 ---
        // 线程池只有一个线程处理
        // 专门负责从队列里取数据并入库
        CompletableFuture<Void> insertFuture = CompletableFuture.runAsync(() -> {
            log.debug("消费者启动");
            // 缓冲区大小
            int capacity = 1000;
            List<SplitChunk> batchBuffer = new ArrayList<>(capacity);
            while (true) {
                try {
                    // take()方法是阻塞的：如果队列空了，它就在这死等，不占用CPU，直到有数据进来
                    // 这比 sleep 轮询要高效得多
                    SplitChunk chunk = chunkQueue.take();

                    // 遇到结束信号
                    if (chunk == stopSplitChunk) {
                        log.debug("收到停止信号通知 消费者结束");
                        break;
                    }

                    batchBuffer.add(chunk);

                    if (batchBuffer.size() >= capacity) {
                        milvusService.batchInsert(batchBuffer);
                        log.debug("批量入库 {} 个分块", batchBuffer.size());
                        batchBuffer.clear(); // 清空缓冲区，准备下一批
                    }
                } catch (InterruptedException e) {
                    // 如果线程被中断（通常是程序结束信号），跳出循环
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("消费者入库异常", e);
                }
            }

            log.debug("当前缓冲区剩余: {}", batchBuffer.size());

            // 【关键点】循环结束后，可能还有剩余的数据没满1000个，必须最后存一次
            if (!batchBuffer.isEmpty()) {
                try {
                    milvusService.batchInsert(batchBuffer);
                    log.debug("最后剩余 {} 个分块入库", batchBuffer.size());
                } catch (Exception e) {
                    log.error("最终入库异常", e);
                }
            }
        }, insertExecutor);// 建议使用独立的线程池，避免和计算任务抢占资源

        // --- 生产者线程池 ---
        List<CompletableFuture<Void>> producerFutures = new ArrayList<>();

        for (Path path : javaFiles) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // 1. 加载并分块
                    List<SplitChunk> chunkList = splitService.loadAndSplit(path, projectName);

                    // 2. 向量化并放入队列
                    for (SplitChunk chunk : chunkList) {
                        List<Float> vector = embeddingService.embedding(chunk.getContent());
                        chunk.setVector(vector);

                        // 【关键点】put() 也是线程安全的。如果队列满了（假设设置了容量），它会阻塞等待
                        chunkQueue.put(chunk);
                    }

                    log.debug("文件 {} 处理完毕，已投入队列", path.getFileName());

                } catch (Exception e) {
                    log.error("处理文件 {} 失败", path, e);
                }
            }, codeProcessExecutor);

            producerFutures.add(future);
        }

        // 3. 等待所有生产者完成任务
        CompletableFuture.allOf(producerFutures.toArray(new CompletableFuture[0]))
//                .thenRun(() -> {
//                    try {
//                        chunkQueue.put(stopSplitChunk);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
//                })
//                .thenCompose(v -> insertFuture)
                .join();
        log.debug("所有生产者任务完成，耗时 {}s，等待消费者...", (System.currentTimeMillis() - startTime)/1000.0);

        // 4. 通知消费者线程结束
        chunkQueue.put(stopSplitChunk);
        // 等待消费者线程结束
        insertFuture.join();
        log.debug("所有任务完成，耗时 {}s", (System.currentTimeMillis() - startTime)/1000.0);


    }

}
