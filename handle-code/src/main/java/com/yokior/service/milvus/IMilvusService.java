package com.yokior.service.milvus;

import com.yokior.common.EmbedSearchResult;
import com.yokior.common.SplitChunk;

import java.util.List;
import java.util.Map;

/**
 * @author Yokior
 * @description
 * @date 2026/1/7 15:04
 */
public interface IMilvusService {

    /**
     * 插入数据
     */
    void insert(List<Float> vector, String content, String type, String site, String project_name, String className, String methodName);

    void insert(SplitChunk splitChunk);

    /**
     * 批量插入数据
     */
    void batchInsert(List<List<Float>> vectors, List<String> contents, List<String> types, List<String> sites, String project_name, List<String> classNames, List<String> methodNames);

    void batchInsert(List<SplitChunk> splitChunks);

    List<EmbedSearchResult> search(List<Float> vector, int topK);

    List<EmbedSearchResult> search(List<Float> vector, int topK, String expr);

    List<EmbedSearchResult> search(List<Float> vector, int topK, String expr, Map<String, Object> params);
}
