package com.min.meow.post.service;

import com.min.meow.post.repository.LostCatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LostCatPostCountCacheService {

    private final LostCatRepository lostCatRepository;

    // 전체 실종글 수 캐싱 (TTL 5분)
    @Cacheable(cacheNames = "post:lost:count", key = "'all'")
    public long countAll() {
        return lostCatRepository.countAllPosts();
    }

    @CacheEvict(cacheNames = "post:lost:count", key = "'all'")
    public void evict() {}
}
