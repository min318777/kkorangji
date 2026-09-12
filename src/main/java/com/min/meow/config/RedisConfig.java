package com.min.meow.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.min.meow.notification.sse.SseRedisSubscriber;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import org.springframework.data.redis.core.RedisTemplate;

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
        // ObjectMapper에 타입 정보 보존 설정이 없으면 발생하는 문제:
        // - 타입 정보 손실: GenericJackson2JsonRedisSerializer가 복잡한 제네릭 타입(예: Page<XxxResponse>)을 Redis에 저장할 때 타입 정보를 잃어버림
        // - 역직렬화 실패: 캐시에서 꺼낼 때 LinkedHashMap으로 변환되어 원래 제네릭 타입으로 캐스팅 불가
        // 해결: activateDefaultTyping()으로 JSON에 타입 정보를 함께 저장 → 정확한 타입으로 복원

        // 기본 캐시 설정 (defaultTtl 사용)
        RedisCacheConfiguration defaultConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(Duration.ofSeconds(defaultTtl))
                .serializeKeysWith(RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(cacheMapper)));

        // 마이페이지 통계 캐시 설정 (TTL 10분)
        // 자랑글 수, 실종글 수, 댓글 수 — 실시간성 낮음
        // 무효화 트리거: 게시글/댓글 작성·삭제 시
        RedisCacheConfiguration userStatsConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(cacheMapper)));

        // 인기글 캐시 설정 (TTL 30초) — 스탬피드 방지 테스트 주기에 맞춤
        RedisCacheConfiguration popularPostsConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(Duration.ofSeconds(30))
                .serializeKeysWith(RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(cacheMapper)));

        // 상세조회 캐시 설정 (TTL 30초) — 스탬피드 테스트용
        RedisCacheConfiguration postDetailConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(Duration.ofSeconds(30))
                .serializeKeysWith(RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(cacheMapper)));

        // 게시글 전체 수 캐시 (TTL 5분) — COUNT(*) 쿼리 대체
        RedisCacheConfiguration postCountConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext
                        .SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(cacheMapper)));

        return RedisCacheManager.RedisCacheManagerBuilder
                .fromConnectionFactory(redisConnectionFactory)
                .cacheDefaults(defaultConfiguration)
                // 마이페이지 통계 캐시 (TTL 10분)
                .withCacheConfiguration("user:stats", userStatsConfiguration)
                // 인기글 목록 캐시 — v1(무방지, 비교 기준선) 30초 TTL
                .withCacheConfiguration("post:boast:popular", popularPostsConfiguration)
                // 상세조회 캐시 — v1/v2/v3 공용 (TTL 30초)
                .withCacheConfiguration("post:boast:detail", postDetailConfiguration)
                // 자랑글 전체 수 캐시 (TTL 5분)
                .withCacheConfiguration("post:boast:count", postCountConfiguration)
                // 실종글 전체 수 캐시 (TTL 5분)
                .withCacheConfiguration("post:lost:count", postCountConfiguration)
                .build();
    }


    /**
     * 조회수 캐싱 및 기타 Redis 작업을 위한 RedisTemplate
     * 용도:
     * - 조회수 증가 (INCR 명령어로 원자적 증가)
     * - 중복 조회 방지 (SETNX로 클라이언트별 debounce)
     * - 배치 플러시를 위한 조회수 집계
     * Serializer 설정:
     * - Key: StringRedisSerializer (읽기 쉬운 문자열 키)
     * - Value: StringRedisSerializer (숫자를 문자열로 저장, INCR 호환)
     */
    /**
     * Redisson 클라이언트 (분산 락용)
     * redisson-spring-boot-starter가 빈 문자열 password를 AUTH로 보내는 문제 방지
     * password가 없으면 setPassword 호출 생략
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + redisHost + ":" + redisPort;
        var serverConfig = config.useSingleServer().setAddress(address);
        // 비밀번호가 있을 때만 AUTH 설정 (빈 문자열이면 AUTH 생략)
        if (redisPassword != null && !redisPassword.isBlank()) {
            serverConfig.setPassword(redisPassword);
        }
        return Redisson.create(config);
    }

    // SSE 알림 채널 구독 컨테이너 — "sse:notify:*" 패턴의 메시지를 SseRedisSubscriber로 전달
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory, SseRedisSubscriber sseRedisSubscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(sseRedisSubscriber, new PatternTopic("sse:notify:*"));
        return container;
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Key와 Value 모두 String으로 직렬화 (INCR 명령어 호환)
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}
