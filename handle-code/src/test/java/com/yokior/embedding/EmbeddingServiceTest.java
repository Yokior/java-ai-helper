package com.yokior.embedding;

import com.yokior.service.embedding.IEmbeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

/**
 * @author Yokior
 * @description
 * @date 2026/1/8 16:36
 */
@SpringBootTest
public class EmbeddingServiceTest {

    @Autowired
    private IEmbeddingService embeddingService;


    @Test
    void embedding() {
        List<Float> floats = embeddingService.embedding("计算用户问题与文档段落之间的BM25相似度分数");
        System.out.println(floats);
    }
}
