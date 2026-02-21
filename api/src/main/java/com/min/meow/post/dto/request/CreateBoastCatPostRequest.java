package com.min.meow.post.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "자랑글 생성 요청")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateBoastCatPostRequest {

    // 제목: 2~100자, 필수
    @Schema(description = "게시글 제목 (2~100자)", example = "우리 고양이 너무 귀여워요!")
    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(min = 2, max = 100, message = "제목은 2자 이상 100자 이하로 입력해주세요.")
    private String title;

    // 본문: 최대 2000자, 선택 (자랑글은 사진만으로도 작성 가능)
    @Schema(description = "게시글 본문 (최대 2000자, 선택)", example = "오늘 우리 냥이가 낮잠 자는 모습이에요~")
    @Size(max = 2000, message = "내용은 2000자 이하로 입력해주세요.")
    private String content;

    /**
     * S3에 업로드된 이미지의 key 목록
     * Presigned URL로 업로드 완료 후 받은 key를 전달
     */
    @Schema(description = "S3 업로드 후 받은 이미지 key 목록 (최대 10장)",
            example = "[\"meow/uuid-1.jpg\", \"meow/uuid-2.png\"]")
    @Size(max = 10, message = "이미지는 최대 10장까지 업로드 가능합니다.")
    private List<String> imageKeys;
}
