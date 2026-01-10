package com.yokior.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Yokior
 * @description 向量搜索结果
 * @date 2026/1/10 15:52
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmbedSearchResult {

    private Long id;

    private String content;

    private String type;

    private String className;

    private String methodName;

    private String site;

    private String projectName;

    private Float score;

}
