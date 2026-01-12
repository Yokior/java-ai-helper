package com.yokior.tool;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.function.BiFunction;

/**
 * @author Yokior
 * @description
 * @date 2026/1/11 17:42
 */
public class SearchTool implements BiFunction<String, ToolContext, String> {
    @Override
    public String apply(@ToolParam(description = "查询内容") String query, ToolContext toolContext) {

        return "搜索结果：8422513110";
    }
}
