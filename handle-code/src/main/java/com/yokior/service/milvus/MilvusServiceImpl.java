package com.yokior.service.milvus;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.yokior.common.EmbedSearchResult;
import com.yokior.common.SplitChunk;
import com.yokior.utils.MilvusUtils;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.vector.response.SearchResp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    @Override
    public List<EmbedSearchResult> search(List<Float> vector, int topK) {

        SearchResp searchResp = milvusUtils.search("JavaProject", List.of(vector), topK, IndexParam.MetricType.COSINE);

        // 封装结果
        List<EmbedSearchResult> embedSearchResultList = new ArrayList<>();

        for (SearchResp.SearchResult data : searchResp.getSearchResults().get(0)) {
            EmbedSearchResult embedSearchResult = new EmbedSearchResult();
            Map<String, Object> entity = data.getEntity();
            embedSearchResult.setId((Long) data.getId());
            embedSearchResult.setScore(data.getScore());
            embedSearchResult.setContent((String) entity.get("content"));
            embedSearchResult.setType((String) entity.get("type"));
            embedSearchResult.setClassName((String) entity.get("class_name"));
            embedSearchResult.setMethodName((String) entity.get("method_name"));
            embedSearchResult.setSite((String) entity.get("site"));
            embedSearchResult.setProjectName((String) entity.get("project_name"));

            embedSearchResultList.add(embedSearchResult);
        }

        return embedSearchResultList;
    }

    @Override
    public List<EmbedSearchResult> search(List<Float> vector, int topK, String expr) {

        SearchResp searchResp = milvusUtils.search("JavaProject", List.of(vector), topK, IndexParam.MetricType.COSINE, expr);

        // 封装结果
        List<EmbedSearchResult> embedSearchResultList = new ArrayList<>();

        for (SearchResp.SearchResult data : searchResp.getSearchResults().get(0)) {
            EmbedSearchResult embedSearchResult = new EmbedSearchResult();
            Map<String, Object> entity = data.getEntity();
            embedSearchResult.setId((Long) data.getId());
            embedSearchResult.setScore(data.getScore());
            embedSearchResult.setContent((String) entity.get("content"));
            embedSearchResult.setType((String) entity.get("type"));
            embedSearchResult.setClassName((String) entity.get("class_name"));
            embedSearchResult.setMethodName((String) entity.get("method_name"));
            embedSearchResult.setSite((String) entity.get("site"));
            embedSearchResult.setProjectName((String) entity.get("project_name"));

            embedSearchResultList.add(embedSearchResult);
        }

        return embedSearchResultList;
    }

    @Override
    public List<EmbedSearchResult> search(List<Float> vector, int topK, String expr, Map<String, Object> params) {

        SearchResp searchResp = milvusUtils.search("JavaProject", List.of(vector), topK, IndexParam.MetricType.COSINE, expr, params);

        // 封装结果
        List<EmbedSearchResult> embedSearchResultList = new ArrayList<>();

        for (SearchResp.SearchResult data : searchResp.getSearchResults().get(0)) {
            EmbedSearchResult embedSearchResult = new EmbedSearchResult();
            Map<String, Object> entity = data.getEntity();
            embedSearchResult.setId((Long) data.getId());
            embedSearchResult.setScore(data.getScore());
            embedSearchResult.setContent((String) entity.get("content"));
            embedSearchResult.setType((String) entity.get("type"));
            embedSearchResult.setClassName((String) entity.get("class_name"));
            embedSearchResult.setMethodName((String) entity.get("method_name"));
            embedSearchResult.setSite((String) entity.get("site"));
            embedSearchResult.setProjectName((String) entity.get("project_name"));

            embedSearchResultList.add(embedSearchResult);
        }

        return embedSearchResultList;
    }
}
