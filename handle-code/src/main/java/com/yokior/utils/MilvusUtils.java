package com.yokior.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.collection.request.ReleaseCollectionReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.BaseVector;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.InsertResp;
import io.milvus.v2.service.vector.response.QueryResp;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author Yokior
 * @description Milvus向量数据库工具类
 * @date 2026/1/7 14:41
 */
@Component
@Slf4j
public class MilvusUtils {

    private final MilvusClientV2 milvusClient;
    private final Gson gson = new Gson();

    public MilvusUtils(@Qualifier("myMilvusClient") MilvusClientV2 myMilvusClient) {
        this.milvusClient = myMilvusClient;
    }

    /**
     * 创建集合
     *
     * @param collectionName 集合名称
     * @param dimension 向量维度
     * @return 是否创建成功
     */
    public boolean createCollection(String collectionName, int dimension) {
        try {
            CreateCollectionReq.CollectionSchema schema = CreateCollectionReq.CollectionSchema.builder()
                    .enableDynamicField(false)
                    .build();

            schema.addField(AddFieldReq.builder()
                    .fieldName("id")
                    .dataType(DataType.Int64)
                    .isPrimaryKey(true)
                    .autoID(true)
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName("vector")
                    .dataType(DataType.FloatVector)
                    .dimension(dimension)
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName("content")
                    .dataType(DataType.VarChar)
                    .maxLength(65535)
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName("type")
                    .dataType(DataType.VarChar)
                    .maxLength(100)
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName("site")
                    .dataType(DataType.VarChar)
                    .maxLength(500)
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName("project_name")
                    .dataType(DataType.VarChar)
                    .maxLength(200)
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName("class_name")
                    .dataType(DataType.VarChar)
                    .maxLength(200)
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName("method_name")
                    .dataType(DataType.VarChar)
                    .maxLength(200)
                    .build());

            List<IndexParam> indexes = new ArrayList<>();
            indexes.add(IndexParam.builder()
                    .fieldName("vector")
                    .indexType(IndexParam.IndexType.HNSW)
                    .metricType(IndexParam.MetricType.COSINE)
                    .build());

            CreateCollectionReq requestCreate = CreateCollectionReq.builder()
                    .collectionName(collectionName)
                    .collectionSchema(schema)
                    .indexParams(indexes)
                    .build();

            milvusClient.createCollection(requestCreate);
            return true;
        } catch (Exception e) {
            log.error("创建集合失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 删除集合
     *
     * @param collectionName 集合名称
     * @return 是否删除成功
     */
    public boolean dropCollection(String collectionName) {
        try {
            DropCollectionReq dropCollectionReq = DropCollectionReq.builder()
                    .collectionName(collectionName)
                    .build();

            milvusClient.dropCollection(dropCollectionReq);
            return true;
        } catch (Exception e) {
            log.error("删除集合失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查集合是否存在
     *
     * @param collectionName 集合名称
     * @return 是否存在
     */
    public boolean hasCollection(String collectionName) {
        try {
            HasCollectionReq hasCollectionReq = HasCollectionReq.builder()
                    .collectionName(collectionName)
                    .build();

            return milvusClient.hasCollection(hasCollectionReq);
        } catch (Exception e) {
            log.error("检查集合是否存在失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 加载集合
     *
     * @param collectionName 集合名称
     * @return 是否加载成功
     */
    public boolean loadCollection(String collectionName) {
        try {
            LoadCollectionReq loadCollectionReq = LoadCollectionReq.builder()
                    .collectionName(collectionName)
                    .build();

            milvusClient.loadCollection(loadCollectionReq);
            return true;
        } catch (Exception e) {
            log.error("加载集合失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 释放集合
     *
     * @param collectionName 集合名称
     * @return 是否释放成功
     */
    public boolean releaseCollection(String collectionName) {
        try {
            ReleaseCollectionReq releaseCollectionReq = ReleaseCollectionReq.builder()
                    .collectionName(collectionName)
                    .build();

            milvusClient.releaseCollection(releaseCollectionReq);
            return true;
        } catch (Exception e) {
            log.error("释放集合失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 插入数据
     *
     * @param collectionName 集合名称
     * @param vectors 向量数据
     * @param contents 文本内容
     * @param types 类型
     * @param sites 位置
     * @param project_name 项目名称
     * @param classNames 类名列表
     * @param methodNames 方法名列表
     * @return 是否插入成功
     */
    public boolean insert(String collectionName, List<List<Float>> vectors, List<String> contents,
                         List<String> types, List<String> sites, String project_name,
                         List<String> classNames, List<String> methodNames) {
        try {
            List<JsonObject> data = new ArrayList<>();

            for (int i = 0; i < vectors.size(); i++) {
                Map<String, Object> rowData = new HashMap<>();
                rowData.put("vector", vectors.get(i));
                rowData.put("content", contents.get(i));
                rowData.put("type", types.get(i));
                rowData.put("site", sites.get(i));
                rowData.put("project_name", project_name);
                rowData.put("class_name", classNames.get(i));
                rowData.put("method_name", methodNames.get(i));

                // 将Map转换为Gson的JsonObject
                String jsonStr = gson.toJson(rowData);
                JsonObject jsonObject = gson.fromJson(jsonStr, JsonObject.class);
                data.add(jsonObject);
            }

            InsertReq insertReq = InsertReq.builder()
                    .collectionName(collectionName)
                    .data(data)
                    .build();

            InsertResp response = milvusClient.insert(insertReq);
            return response.getInsertCnt() > 0;
        } catch (Exception e) {
            log.error("插入数据失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 删除数据
     *
     * @param collectionName 集合名称
     * @param ids 要删除的ID列表
     * @return 是否删除成功
     */
    public boolean deleteByIds(String collectionName, List<Long> ids) {
        try {
            String expr = "id in [" + String.join(",", ids.stream().map(String::valueOf).toArray(String[]::new)) + "]";

            DeleteReq deleteReq = DeleteReq.builder()
                    .collectionName(collectionName)
                    .filter(expr)
                    .build();

            milvusClient.delete(deleteReq);
            return true;
        } catch (Exception e) {
            log.error("删除数据失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 根据表达式删除数据
     *
     * @param collectionName 集合名称
     * @param expr 删除表达式
     * @return 是否删除成功
     */
    public boolean deleteByExpr(String collectionName, String expr) {
        try {
            DeleteReq deleteReq = DeleteReq.builder()
                    .collectionName(collectionName)
                    .filter(expr)
                    .build();

            milvusClient.delete(deleteReq);
            return true;
        } catch (Exception e) {
            log.error("根据表达式删除数据失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 查询数据
     *
     * @param collectionName 集合名称
     * @param expr           查询表达式
     * @param outputFields   输出字段
     * @return 查询结果
     */
    public List<QueryResp.QueryResult> query(String collectionName, String expr, List<String> outputFields) {
        try {
            QueryReq queryReq = QueryReq.builder()
                    .collectionName(collectionName)
                    .filter(expr)
                    .outputFields(outputFields)
                    .build();

            QueryResp response = milvusClient.query(queryReq);
            return response.getQueryResults();
        } catch (Exception e) {
            log.error("查询数据失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 向量搜索
     *
     * @param collectionName 集合名称
     * @param vectors 搜索向量
     * @param topK 返回结果数量
     * @param metricType 度量类型
     * @param expr 表达式
     * @param params 参数
     * @return 搜索结果
     */
    public SearchResp search(String collectionName, List<List<Float>> vectors, int topK,
                           IndexParam.MetricType metricType, String expr, Map<String, Object> params) {
        try {
            // 将List<List<Float>>转换为List<BaseVector>
            List<BaseVector> baseVectors = new ArrayList<>();
            for (List<Float> vector : vectors) {
                // 转换Float为float数组
                float[] floatArray = new float[vector.size()];
                for (int i = 0; i < vector.size(); i++) {
                    floatArray[i] = vector.get(i);
                }
                baseVectors.add(new FloatVec(floatArray));
            }

            SearchReq searchReq = SearchReq.builder()
                    .collectionName(collectionName)
                    .data(baseVectors)
                    .annsField("vector")
                    .topK(topK)
                    .outputFields(List.of("content", "class_name", "method_name", "type", "site", "project_name"))
                    .metricType(metricType)
                    .filter(expr)
                    .searchParams(params)
                    .build();

            return milvusClient.search(searchReq);
        } catch (Exception e) {
            log.error("向量搜索失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 向量搜索
     *
     * @param collectionName 集合名称
     * @param vectors 搜索向量
     * @param topK 返回结果数量
     * @param metricType 度量类型
     * @param expr 表达式
     * @return 搜索结果
     */
    public SearchResp search(String collectionName, List<List<Float>> vectors, int topK,
                             IndexParam.MetricType metricType, String expr) {
        try {
            // 将List<List<Float>>转换为List<BaseVector>
            List<BaseVector> baseVectors = new ArrayList<>();
            for (List<Float> vector : vectors) {
                // 转换Float为float数组
                float[] floatArray = new float[vector.size()];
                for (int i = 0; i < vector.size(); i++) {
                    floatArray[i] = vector.get(i);
                }
                baseVectors.add(new FloatVec(floatArray));
            }

            SearchReq searchReq = SearchReq.builder()
                    .collectionName(collectionName)
                    .data(baseVectors)
                    .annsField("vector")
                    .topK(topK)
                    .outputFields(List.of("content", "class_name", "method_name", "type", "site", "project_name"))
                    .metricType(metricType)
                    .filter(expr)
                    .build();

            return milvusClient.search(searchReq);
        } catch (Exception e) {
            log.error("向量搜索失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 向量搜索（无表达式）
     *
     * @param collectionName 集合名称
     * @param vectors 搜索向量
     * @param topK 返回结果数量
     * @param metricType 度量类型
     * @return 搜索结果
     */
    public SearchResp search(String collectionName, List<List<Float>> vectors, int topK,
                           IndexParam.MetricType metricType) {
        try {
            // 将List<List<Float>>转换为List<BaseVector>
            List<BaseVector> baseVectors = new ArrayList<>();
            for (List<Float> vector : vectors) {
                // 转换Float为float数组
                float[] floatArray = new float[vector.size()];
                for (int i = 0; i < vector.size(); i++) {
                    floatArray[i] = vector.get(i);
                }
                baseVectors.add(new FloatVec(floatArray));
            }

            SearchReq searchReq = SearchReq.builder()
                    .collectionName(collectionName)
                    .data(baseVectors)
                    .annsField("vector")
                    .metricType(metricType)
                    .topK(topK)
                    .outputFields(List.of("content", "class_name", "method_name", "type", "site", "project_name"))
                    .build();

            return milvusClient.search(searchReq);
        } catch (Exception e) {
            log.error("向量搜索失败: {}", e.getMessage());
            return null;
        }
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