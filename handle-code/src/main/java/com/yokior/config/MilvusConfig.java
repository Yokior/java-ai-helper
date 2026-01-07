package com.yokior.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class MilvusConfig {

    @Value("${milvus.host}")
    private String host;

    @Value("${milvus.port}")
    private int port;

    @Bean(destroyMethod = "close")
    public MilvusServiceClient milvusClient() {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                // 1. 连接超时：10秒，避免连接时卡死
                .withConnectTimeout(10, TimeUnit.SECONDS)
                // 2. 心跳保活：55秒，防止因闲置被服务端断开（Milvus默认超时通常是60秒）
                .withKeepAliveTime(55, TimeUnit.SECONDS)
                .withDatabaseName("default")
                .build();
        
        return new MilvusServiceClient(connectParam);
    }
}
