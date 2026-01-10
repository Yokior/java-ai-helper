package com.yokior.service.aichat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * @author Yokior
 * @description
 * @date 2026/1/10 22:40
 */
@Service
public class AiChatService implements IAiChatService {

    private ChatClient chatClient;

    public AiChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String test(String userQuery) {
        return chatClient.prompt()
                .user(userQuery)
                .call()
                .content();
    }
}
