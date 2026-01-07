package com.yokior.service.milvus;

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
public class MilvusService implements IMilvusService {

    @Autowired
    private MilvusUtils milvusUtils;

    private static final String COLLECTION_NAME = "JavaProject";

    @Override
    public void insert(List<Float> vector, String content, String type, String site, String project_name, String className, String methodName) {
        milvusUtils.insert(COLLECTION_NAME, List.of(vector), List.of(content), List.of(type), List.of(site), project_name, List.of(className), List.of(methodName));
    }

    @Override
    public void batchInsert(List<List<Float>> vectors, List<String> contents, List<String> types, List<String> sites, String project_name, List<String> classNames, List<String> methodNames) {
        milvusUtils.insert(COLLECTION_NAME, vectors, contents, types, sites, project_name, classNames, methodNames);
    }
}
