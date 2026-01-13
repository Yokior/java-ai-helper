package com.yokior.entity;

import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yokior.handler.MyJsonbTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.LinkedList;

/**
 * @author Yokior
 * @description 聊天记录实体类
 * @date 2026/1/13 17:29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "chat_log", autoResultMap = true)
public class ChatLog {

    @TableId
    private String conversationId;

    @TableField(typeHandler = MyJsonbTypeHandler.class)
    private LinkedList<Checkpoint> content;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
