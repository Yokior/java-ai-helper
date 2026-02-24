package com.yokior.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Yokior
 * @description 流式响应类
 * @date 2026/2/24 22:16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatStreamResponse {

    /**
     * 响应内容
     */
    private String content;

}
