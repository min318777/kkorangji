package com.min.meow.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

// 카카오 연결 해제(Unlink) Admin API 호출 — 탈퇴 시 카카오 쪽 앱 연결도 함께 해제
@Slf4j
@Component
public class KakaoUnlinkClient {

    private static final String UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${kakao.admin-key:}")
    private String adminKey;

    // 카카오 연결 해제 요청 (best-effort) — 실패해도 탈퇴 자체는 막지 않고 로그만 남김
    public void unlink(String socialId) {
        if (adminKey == null || adminKey.isBlank()) {
            log.warn("카카오 Admin 키가 설정되지 않아 연결 해제 API 호출을 건너뜀 - socialId: {}", socialId);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "KakaoAK " + adminKey);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("target_id_type", "user_id");
        body.add("target_id", socialId);

        try {
            restTemplate.postForEntity(UNLINK_URL, new HttpEntity<>(body, headers), String.class);
            log.info("카카오 연결 해제 API 호출 성공 - socialId: {}", socialId);
        } catch (RestClientException e) {
            log.warn("카카오 연결 해제 API 호출 실패 (탈퇴 처리는 계속 진행) - socialId: {}, error: {}", socialId, e.getMessage());
        }
    }
}
