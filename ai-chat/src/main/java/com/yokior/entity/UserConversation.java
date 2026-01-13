package com.yokior.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Yokior
 * @description
 * @date 2026/1/14 0:15
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("user_conversation")
public class UserConversation {

    private Long userId;

    private String conversationId;

}
