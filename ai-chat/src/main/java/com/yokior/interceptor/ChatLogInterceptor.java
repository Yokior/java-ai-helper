package com.yokior.interceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Yokior
 * @description
 * @date 2026/1/11 23:19
 */
@Slf4j
public class ChatLogInterceptor extends ModelInterceptor {
    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {

        log.info("原始请求AI: {}", JSONObject.toJSONString(request, JSONWriter.Feature.PrettyFormat));

        ModelResponse response = handler.call(request);

        log.info("AI原始响应: {}", JSONObject.toJSONString(response, JSONWriter.Feature.PrettyFormat));

        return response;
    }

    @Override
    public String getName() {
        return "ChatLogInterceptor";
    }
}
