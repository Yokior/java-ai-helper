package com.yokior.tool;

import com.yokior.common.CodeFragmentType;
import com.yokior.common.EmbedSearchResult;
import com.yokior.common.FilterExpression;
import com.yokior.service.embedding.IEmbeddingService;
import com.yokior.service.milvus.IMilvusService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Yokior
 * @description
 * @date 2026/1/17 21:15
 */
@Component
@Slf4j
public class MilvusSearchTool {

    @Autowired
    private IMilvusService milvusService;

    @Autowired
    private IEmbeddingService embeddingService;

    @Tool(name = "检索向量数据库", description = "一个用于检索向量数据库的工具，输入查询内容，返回结果")
    public String search(@ToolParam(description = "需要查询的内容") String query,
                         @ToolParam(required = false, description = "(可选) 类型 可选值：" + CodeFragmentType.ALL_TYPE_VALUES) String type,
                         @ToolParam(required = false, description = "(可选) 所属类名") String className,
                         @ToolParam(required = false, description = "(可选) 所属方法名") String methodName) {

        List<Float> floatList = embeddingService.embedding(query);

        FilterExpression filterExpression = FilterExpression.builder()
                .eq(StringUtils.isNotBlank(type), "type", type)
                .and()
                .eq(StringUtils.isNotBlank(className), "class_name", className)
                .and()
                .eq(StringUtils.isNotBlank(methodName), "method_name", methodName)
                .build();

        log.info("查询条件：{}", filterExpression.getExpression());

        StringBuilder result = new StringBuilder();
        for (EmbedSearchResult item : milvusService.search(floatList, 5, filterExpression.getExpression())) {
            result.append("分数：").append(item.getScore()).append("\n")
                    .append("类型：").append(item.getType()).append("\n")
                    .append("所属项目名：").append(item.getProjectName()).append("\n")
                    .append("所处位置：").append(item.getSite()).append("\n")
                    .append("所属类名：").append(item.getClassName()).append("\n");
            if (StringUtils.isBlank(item.getMethodName())) {
                result.append("所属方法名：").append(item.getMethodName()).append("\n");
            }
            result.append("代码块内容：").append(item.getContent()).append("\n");
        }

//        log.info("查询结果：{}", result);

        return result.toString();
    }

}
