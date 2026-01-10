package com.yokior.advisor;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;

/**
 * @author Yokior
 * @description
 * @date 2026/1/10 23:25
 */
@Slf4j
public class ChatLogAdvisor implements BaseAdvisor {
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        log.info("prompt: {}", JSONObject.toJSONString(chatClientRequest.prompt(), JSONWriter.Feature.PrettyFormat));
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        log.info("AI响应: {}", JSONObject.toJSONString(chatClientResponse.chatResponse(), JSONWriter.Feature.PrettyFormat));
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
