package com.min.meow.post.service;

import com.min.meow.post.repository.BoastCatPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoastCatPostCountCacheService {

    private final BoastCatPostRepository boastCatPostRepository;

    // 전체 게시글 수 캐싱 (TTL 5분)
    // 게시글 생성/삭제 시 evict로 무효화
    @Cacheable(cacheNames = "post:boast:count", key = "'all'")
    public long countAll() {
        return boastCatPostRepository.countAllPosts();
    }

    @CacheEvict(cacheNames = "post:boast:count", key = "'all'")
    public void evict() {}
}