package com.yokior.service.embedding;

import java.util.List;

/**
 * @author Yokior
 * @description
 * @date 2026/1/8 16:06
 */
public interface IEmbeddingService {
    /**
     * 获取文本的嵌入向量
     *
     * @param text
     * @return
     */
    List<Float> embedding(String text);

}
