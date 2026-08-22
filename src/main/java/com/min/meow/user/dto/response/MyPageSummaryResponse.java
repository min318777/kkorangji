package com.min.meow.user.dto.response;

import com.min.meow.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 마이페이지 요약 정보 응답 DTO
 * 사용자 기본 정보와 작성한 글/댓글 통계를 포함
 */
@Schema(description = "마이페이지 요약 응답")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MyPageSummaryResponse {

    @Schema(description = "로그인 ID", example = "cat_lover")
    private String loginId;

    @Schema(description = "닉네임", example = "홍길동")
    private String nickname;

    @Schema(description = "이메일", example = "cat_lover@example.com")
    private String email;

    @Schema(description = "전체 게시글 수 (자랑글 + 실종글)", example = "15")
    private long totalPostCount;

    @Schema(description = "고양이 자랑글 수", example = "10")
    private long boastCatPostCount;

    @Schema(description = "실종 고양이글 수", example = "5")
    private long lostCatPostCount;

    @Schema(description = "전체 댓글 수", example = "30")
    private long totalCommentCount;

    @Schema(description = "가입일시", example = "2025-01-15T10:30:00")
    private LocalDateTime registeredAt;

    public static MyPageSummaryResponse from(User user, long boastCount, long lostCount, long commentCount) {
        return MyPageSummaryResponse.builder()
                .loginId(user.getLoginId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .totalPostCount(boastCount + lostCount)
                .boastCatPostCount(boastCount)
                .lostCatPostCount(lostCount)
                .totalCommentCount(commentCount)
                .registeredAt(user.getRegisteredAt())
                .build();
    }
}
