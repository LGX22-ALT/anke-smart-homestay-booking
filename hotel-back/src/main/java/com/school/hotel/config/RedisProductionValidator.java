package com.school.hotel.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class RedisProductionValidator implements ApplicationRunner {
    private final StringRedisTemplate redisTemplate;

    public RedisProductionValidator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
            connection.ping();
        } catch (Exception exception) {
            throw new IllegalStateException("生产环境必须连接可用的 Redis", exception);
        }
    }
}
