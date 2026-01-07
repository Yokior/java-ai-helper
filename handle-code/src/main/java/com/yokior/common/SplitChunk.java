package com.yokior.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Yokior
 * @description 拆分分片结果
 * @date 2026/1/7 16:59
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SplitChunk {

    /**
     * 分片向量
     */
    private List<Float> vector;

    /**
     * 分片内容
     */
    private String content;

    /**
     * 类名
     */
    private String className;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 分片类型
     */
    private String type;

    /**
     * 所处位置
     */
    private String site;

    /**
     * 项目名
     */
    private String projectName;
}
