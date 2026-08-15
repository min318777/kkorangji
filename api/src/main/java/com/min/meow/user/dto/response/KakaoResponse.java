package com.min.meow.user.dto.response;


import java.util.Map;

/**
 * 카카오 로그인 응답 파싱
 * 동의항목을 아무것도 요청하지 않으므로 id(고유 식별자)만 사용, email/name은 수집하지 않음
 */
public class KakaoResponse implements OAuth2Response{

    private final Map<String, Object> attribute;

    public KakaoResponse(Map<String, Object> attribute){
        this.attribute = attribute;
    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getProviderId() {
        return attribute.get("id").toString();
    }

    @Override
    public String getEmail() {
        // 이메일 동의항목을 요청하지 않으므로 항상 null
        return null;
    }

    @Override
    public String getName() {
        // 닉네임 동의항목을 요청하지 않으므로 항상 null (닉네임은 자동 생성)
        return null;
    }
}
