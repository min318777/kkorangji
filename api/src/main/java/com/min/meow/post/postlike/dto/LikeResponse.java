package com.min.meow.post.postlike.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 좋아요 API 응답 DTO
 *
 * 좋아요 토글 후 클라이언트에게 현재 상태를 전달합니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LikeResponse {

    /**
     * 좋아요 상태
     * - true: 좋아요 등록됨 (방금 좋아요를 눌렀음)
     * - false: 좋아요 취소됨 (방금 좋아요를 취소했음)
     */
    private boolean liked;

    /**
     * 현재 총 좋아요 수
     */
    private Long likeCount;
}
