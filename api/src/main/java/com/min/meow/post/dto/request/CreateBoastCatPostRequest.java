package com.min.meow.post.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * 고양이 자랑글 생성 요청 DTO
 * Presigned URL 기반 이미지 업로드 방식:
 * 1. 클라이언트가 먼저 /api/images/presigned-urls 로 Presigned URL 요청
 * 2. Presigned URL로 S3에 이미지 직접 업로드
 * 3. 업로드 완료 후 받은 S3 key를 imageKeys에 담아서 게시글 생성 요청
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateBoastCatPostRequest {

    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(min = 2, max = 100, message = "제목은 2자 이상 100자 이하로 입력해주세요.")
    private String title;

    @Size(max = 1000, message = "내용은 1000자 이하로 입력해주세요.")
    private String content;

    /**
     * S3에 업로드된 이미지의 key 목록
     * Presigned URL로 업로드 완료 후 받은 key를 전달
     * 예: ["meow/uuid-1.jpg", "meow/uuid-2.png"]
     */
    @Size(max = 10, message = "이미지는 10장 이하로 업로드해주세요.")
    private List<String> imageKeys;
}
