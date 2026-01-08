package com.yokior.service.handlecode;

import com.yokior.common.SplitChunk;
import com.yokior.service.embedding.IEmbeddingService;
import com.yokior.service.milvus.IMilvusService;
import com.yokior.service.split.ISplitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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
}
