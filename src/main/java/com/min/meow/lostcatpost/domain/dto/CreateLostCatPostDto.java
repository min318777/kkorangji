package com.min.meow.lostcatpost.domain.dto;

import com.min.meow.lostcatpost.entity.LostCatPost;
import com.min.meow.lostcatpostcomment.domain.dto.LostCatPostCommentDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLostCatPostDto {

    private Long lostCatPostId;

    private String title;

    private String content;

    private String catName;

    private String catType;

    private String catColor;

    private Integer catAge;

    private Integer catWeight;

    private String catImageUrl;

    private String lostLocation;

    private Double latitude;

    private Double longitude;

    private Integer reward;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static CreateLostCatPostDto convertToDto(LostCatPost lostCatPost){

        return CreateLostCatPostDto.builder()
                .lostCatPostId(lostCatPost.getLostCatPostId())
                .title(lostCatPost.getTitle())
                .content(lostCatPost.getContent())
                .catName(lostCatPost.getCatName())
                .catType(lostCatPost.getCatType())
                .catColor(lostCatPost.getCatColor())
                .catAge(lostCatPost.getCatAge())
                .catWeight(lostCatPost.getCatWeight())
                .catImageUrl(lostCatPost.getCatImageUrl())
                .lostLocation(lostCatPost.getLostLocation())
                .reward(lostCatPost.getReward())
                .latitude(lostCatPost.getLatitude())
                .longitude(lostCatPost.getLongitude())
                .createdAt(lostCatPost.getCreatedAt())
                .updatedAt(lostCatPost.getUpdatedAt())
                .build();
    }
}
