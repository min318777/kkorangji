package com.min.meow.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;


@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Value("${spring.cache.redis.time-to-live}")
    private Long defaultTtl;


    @Bean
    public ObjectMapper objectMapper(){
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }


    @Bean
    public RedisConnectionFactory redisConnectionFactory(){

        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setPort(redisPort);
        redisStandaloneConfiguration.setHostName(redisHost);
        redisStandaloneConfiguration.setPassword(redisPassword);
        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper){

        // 캐시용 ObjectMapper 복사본 생성
        ObjectMapper cacheMapper = objectMapper.copy();
        // 타입 정보 보존 설정 추가 (이게 핵심!)
        cacheMapper.activateDefaultTyping(
                cacheMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        //코드를 보니 ObjectMapper에 타입 정보 보존 설정이 없어서 발생하는 문제예요!
        // 문제 원인
        //타입 정보 손실: GenericJackson2JsonRedisSerializer가 복잡한 제네릭 타입(RestPage<PostDto>)을 Redis에 저장할 때 타입 정보를 잃어버림
        //역직렬화 실패: 캐시에서 가져올 때 LinkedHashMap으로 변환되지만 RestPage<PostDto>로 캐스팅할 수 없음
        // 해결 원리
        //activateDefaultTyping() 설정으로 JSON에 타입 정보를 함께 저장해서 정확한 타입으로 복원할 수 있게 됩니다.

        // 기본 캐시 설정 (defaultTtl 사용)
        RedisCacheConfiguration defaultConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(Duration.ofSeconds(defaultTtl))
                .serializeKeysWith(RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(cacheMapper)));

        // 최근글 목록 캐시 설정 (TTL 5분)
        // 메인페이지 최근글 목록용, 글 작성/수정/삭제 시 즉시 무효화됨
        RedisCacheConfiguration recentPostsConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(5))  // TTL 5분
                .serializeKeysWith(RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(cacheMapper)));

        // 게시글 상세 캐시 설정 (TTL 10분)
        // 개별 게시글 상세 조회용, 수정/삭제 시 해당 게시글만 무효화됨
        RedisCacheConfiguration postDetailConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(10))  // TTL 10분
                .serializeKeysWith(RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(cacheMapper)));

        return RedisCacheManager.RedisCacheManagerBuilder
                .fromConnectionFactory(redisConnectionFactory)
                .cacheDefaults(defaultConfiguration)
                // 최근글 목록 캐시 (TTL 5분)
                .withCacheConfiguration("post:boast:recent", recentPostsConfiguration)
                .withCacheConfiguration("post:lost:recent", recentPostsConfiguration)
                // 게시글 상세 캐시 (TTL 10분)
                .withCacheConfiguration("post:boast:detail", postDetailConfiguration)
                .withCacheConfiguration("post:lost:detail", postDetailConfiguration)
                .build();
    }


}
