package com.yokior.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private String redisPort;

    // 如果有密码
    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        // 这里是关键：使用 StringCodec
        // 这会让 Redis 中存储的 Key 和 Value 都是 UTF-8 编码的字符串，
        // 这样在 redis-cli 或其他工具中直接查看就是明文，不会是乱码。
        config.setCodec(new StringCodec());

        // 构建单机节点地址
        String address = "redis://" + redisHost + ":" + redisPort;
        
        config.useSingleServer()
              .setAddress(address)
              .setPassword(redisPassword.isEmpty() ? null : redisPassword);

        return Redisson.create(config);
    }
}