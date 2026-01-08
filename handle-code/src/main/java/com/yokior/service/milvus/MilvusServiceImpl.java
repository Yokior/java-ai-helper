package com.yokior.service.milvus;

import com.yokior.common.SplitChunk;
import com.yokior.utils.MilvusUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Yokior
 * @description
 * @date 2026/1/7 15:04
 */
@Service
public class MilvusServiceImpl implements IMilvusService {

    @Autowired
    private MilvusUtils milvusUtils;

    private static final String COLLECTION_NAME = "JavaProject";

    @Override
    public void insert(List<Float> vector, String content, String type, String site, String project_name, String className, String methodName) {
        milvusUtils.insert(COLLECTION_NAME, List.of(vector), List.of(content), List.of(type), List.of(site), project_name, List.of(className), List.of(methodName));
    }

    @Override
    public void insert(SplitChunk splitChunk) {
        milvusUtils.insert(COLLECTION_NAME, List.of(splitChunk.getVector()), List.of(splitChunk.getContent()), List.of(splitChunk.getType()), List.of(splitChunk.getSite()), splitChunk.getProjectName(), List.of(splitChunk.getClassName()), List.of(splitChunk.getMethodName()));
    }

    @Override
    public void batchInsert(List<List<Float>> vectors, List<String> contents, List<String> types, List<String> sites, String project_name, List<String> classNames, List<String> methodNames) {
        milvusUtils.insert(COLLECTION_NAME, vectors, contents, types, sites, project_name, classNames, methodNames);
    }

    @Override
    public void batchInsert(List<SplitChunk> splitChunks) {
        milvusUtils.insert(COLLECTION_NAME, splitChunks.stream().map(SplitChunk::getVector).toList(), splitChunks.stream().map(SplitChunk::getContent).toList(), splitChunks.stream().map(SplitChunk::getType).toList(), splitChunks.stream().map(SplitChunk::getSite).toList(), splitChunks.get(0).getProjectName(), splitChunks.stream().map(SplitChunk::getClassName).toList(), splitChunks.stream().map(SplitChunk::getMethodName).toList());
    }
}
