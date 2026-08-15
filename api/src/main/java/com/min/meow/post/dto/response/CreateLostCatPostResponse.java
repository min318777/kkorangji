package com.min.meow.post.dto.response;

import com.min.meow.post.entity.LostCatPost;
import com.min.meow.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "실종글 작성 응답")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLostCatPostResponse {

    @Schema(description = "게시글 ID", example = "1")
    private Long id;

    @Schema(description = "게시글 제목", example = "우리 고양이를 찾아주세요")
    private String title;

    @Schema(description = "게시글 내용", example = "어제 저녁부터 보이지 않습니다")
    private String content;

    @Schema(description = "고양이 이름", example = "나비")
    private String catName;

    @Schema(description = "작성자 로그인 ID", example = "cat_lover")
    private String writer;

    @Schema(description = "고양이 품종", example = "코리안숏헤어")
    private String catType;

    @Schema(description = "고양이 색상", example = "치즈")
    private String catColor;

    @Schema(description = "고양이 나이 (세)", example = "3")
    private Integer catAge;

    @Schema(description = "고양이 몸무게 (kg)", example = "5")
    private Integer catWeight;

    @Schema(description = "이미지 URL 목록")
    private List<String> imageUrls;

    @Schema(description = "실종 장소", example = "서울시 강남구 역삼동")
    private String lostLocation;

    @Schema(description = "실종 위치 위도", example = "37.5665")
    private Double latitude;

    @Schema(description = "실종 위치 경도", example = "126.9780")
    private Double longitude;

    @Schema(description = "사례금 (원)", example = "100000")
    private Integer reward;

    @Schema(description = "작성일시", example = "2025-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-01-15T10:30:00")
    private LocalDateTime updatedAt;

    public static CreateLostCatPostResponse from(LostCatPost lostCatPost){

        return CreateLostCatPostResponse.builder()
                .id(lostCatPost.getId())
                .title(lostCatPost.getTitle())
                .content(lostCatPost.getContents())
                .catName(lostCatPost.getCatName())
                .writer(lostCatPost.getUser().getNickname())
                .catType(lostCatPost.getCatType())
                .catColor(lostCatPost.getCatColor())
                .catAge(lostCatPost.getCatAge())
                .catWeight(lostCatPost.getCatWeight())
                .imageUrls(lostCatPost.getImageUrls())
                .lostLocation(lostCatPost.getLostLocation())
                .reward(lostCatPost.getReward())
                .latitude(lostCatPost.getLatitude())
                .longitude(lostCatPost.getLongitude())
                .createdAt(lostCatPost.getCreatedAt())
                .updatedAt(lostCatPost.getUpdatedAt())
                .build();
    }
}
