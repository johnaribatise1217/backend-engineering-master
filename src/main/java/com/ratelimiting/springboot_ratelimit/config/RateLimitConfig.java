package com.ratelimiting.springboot_ratelimit.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

@Configuration
public class RateLimitConfig {

  @Value("${spring.data.redis.host}")
  private String redisHost;
  @Value ("${spring.data.redis.port}")
  private int redisPort;
  @Value("${spring.data.redis.username}")
  private String redisUsername;
  @Value("${spring.data.redis.password}")
  private String password;

  @Bean
  @SuppressWarnings("unused")
  RedisClient redisClient() {
    return RedisClient.create(
      RedisURI.builder()
        .withHost(redisHost)
        .withPort(redisPort)
        .withAuthentication(redisUsername, password)
        .build()
    );
  }

  @Bean 
  ProxyManager<String> proxyManager(RedisClient redisClient){
    var redisConnection = redisClient.connect(
      RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
    );
    //REDUCE TTL FOR BUCKETS
    var expirationStrategy = ExpirationAfterWriteStrategy
      .basedOnTimeForRefillingBucketUpToMax(Duration.ofHours(1));

    var clientConfig = ClientSideConfig.getDefault()
      .withExpirationAfterWriteStrategy(expirationStrategy);

    //build the proxy manager
    return LettuceBasedProxyManager.builderFor(redisConnection)
      .withClientSideConfig(clientConfig)
      .build();
  }
}
