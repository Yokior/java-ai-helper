package com.yokior.utils;

import com.alibaba.fastjson2.JSONObject;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.*;
import io.milvus.param.*;
import io.milvus.param.collection.*;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.index.DropIndexParam;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author Yokior
 * @description Milvus向量数据库工具类
 * @date 2026/1/7 14:41
 */
@Component
public class MilvusUtils {

    private final MilvusServiceClient milvusClient;

    public MilvusUtils(MilvusServiceClient milvusClient) {
        this.milvusClient = milvusClient;
    }

    /**
     * 创建集合
     *
     * @param collectionName 集合名称
     * @param dimension 向量维度
     * @return 是否创建成功
     */
    public boolean createCollection(String collectionName, int dimension) {
        FieldType fieldType1 = FieldType.newBuilder()
                .withName("id")
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(true)
                .build();

        FieldType fieldType2 = FieldType.newBuilder()
                .withName("vector")
                .withDataType(DataType.FloatVector)
                .withDimension(dimension)
                .build();

        FieldType fieldType3 = FieldType.newBuilder()
                .withName("content")
                .withDataType(DataType.VarChar)
                .withMaxLength(65535)
                .build();

        CreateCollectionParam createCollectionReq = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withDescription("代码片段向量集合")
                .withShardsNum(2)
                .addFieldType(fieldType1)
                .addFieldType(fieldType2)
                .addFieldType(fieldType3)
                .build();

        R<RpcStatus> response = milvusClient.createCollection(createCollectionReq);
        return response.getStatus() == R.Status.Success.getCode();
    }

    /**
     * 删除集合
     *
     * @param collectionName 集合名称
     * @return 是否删除成功
     */
    public boolean dropCollection(String collectionName) {
        DropCollectionParam dropCollectionReq = DropCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();

        R<RpcStatus> response = milvusClient.dropCollection(dropCollectionReq);
        return response.getStatus() == R.Status.Success.getCode();
    }

    /**
     * 检查集合是否存在
     *
     * @param collectionName 集合名称
     * @return 是否存在
     */
    public boolean hasCollection(String collectionName) {
        HasCollectionParam hasCollectionReq = HasCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();

        R<Boolean> response = milvusClient.hasCollection(hasCollectionReq);
        return response.getStatus() == R.Status.Success.getCode() && response.getData();
    }

    /**
     * 加载集合
     *
     * @param collectionName 集合名称
     * @return 是否加载成功
     */
    public boolean loadCollection(String collectionName) {
        LoadCollectionParam loadCollectionReq = LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();

        R<RpcStatus> response = milvusClient.loadCollection(loadCollectionReq);
        return response.getStatus() == R.Status.Success.getCode();
    }

    /**
     * 释放集合
     *
     * @param collectionName 集合名称
     * @return 是否释放成功
     */
    public boolean releaseCollection(String collectionName) {
        ReleaseCollectionParam releaseCollectionReq = ReleaseCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();

        R<RpcStatus> response = milvusClient.releaseCollection(releaseCollectionReq);
        return response.getStatus() == R.Status.Success.getCode();
    }

    /**
     * 创建索引
     *
     * @param collectionName 集合名称
     * @param fieldName 字段名称
     * @param indexType 索引类型
     * @param metricType 度量类型
     * @return 是否创建成功
     */
    public boolean createIndex(String collectionName, String fieldName, IndexType indexType, MetricType metricType) {
        CreateIndexParam createIndexReq = CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName(fieldName)
                .withIndexType(indexType)
                .withMetricType(metricType)
                .withExtraParam(new JSONObject().toJSONString())
                .build();

        R<RpcStatus> response = milvusClient.createIndex(createIndexReq);
        return response.getStatus() == R.Status.Success.getCode();
    }

    /**
     * 删除索引
     *
     * @param collectionName 集合名称
     * @param fieldName 字段名称
     * @return 是否删除成功
     */
    public boolean dropIndex(String collectionName, String fieldName) {
        DropIndexParam dropIndexReq = DropIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withIndexName(fieldName)
                .build();

        R<RpcStatus> response = milvusClient.dropIndex(dropIndexReq);
        return response.getStatus() == R.Status.Success.getCode();
    }

    /**
     * 插入数据
     *
     * @param collectionName 集合名称
     * @param vectors 向量数据
     * @param contents 文本内容
     * @return 是否插入成功
     */
    public boolean insert(String collectionName, List<List<Float>> vectors, List<String> contents, List<String> types, List<String> sites, String project_name, List<String> classNames, List<String> methodNames) {
        List<InsertParam.Field> fields = new ArrayList<>();

        fields.add(new InsertParam.Field("vector", vectors));
        fields.add(new InsertParam.Field("content", contents));
        fields.add(new InsertParam.Field("type", types));
        fields.add(new InsertParam.Field("site", sites));
        fields.add(new InsertParam.Field("project_name", Collections.nCopies(types.size(), project_name)));
        fields.add(new InsertParam.Field("class_name", classNames));
        fields.add(new InsertParam.Field("method_name", methodNames));

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build();

        R<MutationResult> response = milvusClient.insert(insertParam);
        return response.getStatus() == R.Status.Success.getCode();
    }

    /**
     * 批量插入数据
     *
     * @param collectionName 集合名称
     * @param batchSize 批次大小
     * @param vectors 向量数据
     * @param contents 文本内容
     * @return 是否插入成功
     */
//    public boolean batchInsert(String collectionName, int batchSize, List<List<Float>> vectors, List<String> contents) {
//        if (vectors.size() != contents.size()) {
//            throw new IllegalArgumentException("向量和内容数量不匹配");
//        }
//
//        int totalSize = vectors.size();
//        boolean allSuccess = true;
//
//        for (int i = 0; i < totalSize; i += batchSize) {
//            int endIndex = Math.min(i + batchSize, totalSize);
//
//            List<List<Float>> batchVectors = vectors.subList(i, endIndex);
//            List<String> batchContents = contents.subList(i, endIndex);
//
//            boolean success = insert(collectionName, batchVectors, batchContents);
//            if (!success) {
//                allSuccess = false;
//            }
//        }
//
//        return allSuccess;
//    }

    /**
     * 删除数据
     *
     * @param collectionName 集合名称
     * @param ids 要删除的ID列表
     * @return 是否删除成功
     */
    public boolean deleteByIds(String collectionName, List<Long> ids) {
        String expr = "id in " + ids.toString().replace("[", "(").replace("]", ")");

        DeleteParam deleteParam = DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(expr)
                .build();

        R<MutationResult> response = milvusClient.delete(deleteParam);
        return response.getStatus() == R.Status.Success.getCode();
    }

    /**
     * 根据表达式删除数据
     *
     * @param collectionName 集合名称
     * @param expr 删除表达式
     * @return 是否删除成功
     */
    public boolean deleteByExpr(String collectionName, String expr) {
        DeleteParam deleteParam = DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(expr)
                .build();

        R<MutationResult> response = milvusClient.delete(deleteParam);
        return response.getStatus() == R.Status.Success.getCode();
    }

    /**
     * 查询数据
     *
     * @param collectionName 集合名称
     * @param expr 查询表达式
     * @param outputFields 输出字段
     * @return 查询结果
     */
    public List<Map<String, Object>> query(String collectionName, String expr, List<String> outputFields) {
        QueryParam queryParam = QueryParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(expr)
                .withOutFields(outputFields)
                .build();

        R<QueryResults> response = milvusClient.query(queryParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> results = new ArrayList<>();
        QueryResults queryResults = response.getData();

        for (int i = 0; i < queryResults.getFieldsDataCount(); i++) {
            Map<String, Object> row = new HashMap<>();
            for (String field : outputFields) {
                // 这里需要根据字段类型进行相应的数据提取
                // 简化处理，实际使用时需要更详细的字段数据处理
                row.put(field, queryResults.getFieldsData(i).getScalars());
            }
            results.add(row);
        }

        return results;
    }

    /**
     * 向量搜索
     *
     * @param collectionName 集合名称
     * @param vectors        搜索向量
     * @param topK           返回结果数量
     * @param metricType     度量类型
     * @return 搜索结果
     */
    public SearchResults search(String collectionName, List<List<Float>> vectors, int topK, MetricType metricType) {
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withMetricType(metricType)
                .withTopK(topK)
                .withVectors(vectors)
                .withVectorFieldName("vector")
                .withParams(new JSONObject().toJSONString())
                .addOutField("content")
                .build();

        R<SearchResults> response = milvusClient.search(searchParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            return null;
        }

        List<List<Map<String, Object>>> results = new ArrayList<>();

        return response.getData();

        // 处理搜索结果
//        for (int i = 0; i < searchResults.getResults().getIds().getIntId().getDataCount(); i++) {
//            List<Map<String, Object>> resultItem = new ArrayList<>();
//            // 这里需要根据实际的搜索结果结构进行数据处理
//            // 简化处理，实际使用时需要更详细的搜索结果解析
//            resultItem.add(new HashMap<>());
//            results.add(resultItem);
//        }

    }


    /**
     * 关闭客户端连接
     */
    public void close() {
        if (milvusClient != null) {
            milvusClient.close();
        }
    }
}
