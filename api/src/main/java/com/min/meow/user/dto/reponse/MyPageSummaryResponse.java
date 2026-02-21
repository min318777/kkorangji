package com.min.meow.user.dto.reponse;

import com.min.meow.user.entity.User;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 마이페이지 요약 정보 응답 DTO
 * 사용자 기본 정보와 작성한 글/댓글 통계를 포함
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MyPageSummaryResponse {

    private String loginId;           // 회원 아이디
    private String name;               // 회원 이름
    private String email;              // 이메일
    private long totalPostCount;       // 전체 게시글 수 (자랑글 + 실종글)
    private long boastCatPostCount;    // 고양이 자랑글 수
    private long lostCatPostCount;     // 실종 고양이글 수
    private long totalCommentCount;    // 전체 댓글 수
    private LocalDateTime registeredAt; // 가입일
    
    public static MyPageSummaryResponse from(User user, long boastCount, long lostCount, long commentCount) {
        return MyPageSummaryResponse.builder()
                .loginId(user.getLoginId())
                .name(user.getName())
                .email(user.getEmail())
                .totalPostCount(boastCount + lostCount)
                .boastCatPostCount(boastCount)
                .lostCatPostCount(lostCount)
                .totalCommentCount(commentCount)
                .registeredAt(user.getRegisteredAt())
                .build();
    }
}