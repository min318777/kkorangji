package com.min.meow.post.dto.response;

import com.min.meow.post.entity.LostCatPost;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 실종글 상세 조회 응답 DTO
 *
 * 캐싱을 위해 정적 데이터만 포함:
 * - 댓글은 별도 API로 조회: GET /api/meow/lost-cat/comments/{postId}
 * - 조회수는 별도 API로 증가: POST /api/meow/lost-cat/{postId}/view
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetLostCatPostResponse {

    private Long id;
    private String title;
    private String content;
    private String writer;
    private Long userId;
    private String catName;
    private String catType;
    private String catColor;
    private Integer catAge;
    private Integer catWeight;
    private List<String> imageUrls;
    private String lostLocation;
    private Double latitude;
    private Double longitude;
    private Integer reward;
    private int commentCount;
    private int view;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GetLostCatPostResponse toResponse(LostCatPost lostCatPost){

        return GetLostCatPostResponse.builder()
                .id(lostCatPost.getId())
                .title(lostCatPost.getTitle())
                .content(lostCatPost.getContents())
                .writer(lostCatPost.getUser().getLoginId())
                .userId(lostCatPost.getUser().getId())
                .catName(lostCatPost.getCatName())
                .catType(lostCatPost.getCatType())
                .catColor(lostCatPost.getCatColor())
                .catAge(lostCatPost.getCatAge())
                .catWeight(lostCatPost.getCatWeight())
                .imageUrls(new ArrayList<>(lostCatPost.getImageUrls()))
                .lostLocation(lostCatPost.getLostLocation())
                .reward(lostCatPost.getReward())
                .latitude(lostCatPost.getLatitude())
                .longitude(lostCatPost.getLongitude())
                .commentCount(lostCatPost.getCommentCount())
                .view(lostCatPost.getView())
                .createdAt(lostCatPost.getCreatedAt())
                .updatedAt(lostCatPost.getUpdatedAt())
                .build();
    }
}
