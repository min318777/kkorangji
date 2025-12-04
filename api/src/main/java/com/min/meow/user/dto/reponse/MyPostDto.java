package com.min.meow.user.dto.reponse;

import com.min.meow.post.entity.BoastCatPost;
import com.min.meow.post.entity.LostCatPost;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 내가 쓴 게시글 정보 DTO
 * 고양이 자랑글과 실종 고양이글을 통합하여 표현
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MyPostDto {

    private Long postId;              // 게시글 ID
    private String postType;          // 게시글 타입: "BOAST" 또는 "LOST"
    private String title;             // 제목
    private String contents;          // 내용
    private int view;                 // 조회수
    private int commentCount;         // 댓글 수
    private Integer likeCount;        // 좋아요 수 (자랑글만 해당, null 가능)
    private Boolean isCompleted;      // 완료 여부 (실종글만 해당, null 가능)
    private LocalDateTime createdAt;  // 작성일
    private LocalDateTime updatedAt;  // 수정일

    /**
     * BoastCatPost 엔티티를 DTO로 변환
     */
    public static MyPostDto fromBoastCatPost(BoastCatPost post) {
        return MyPostDto.builder()
                .postId(post.getId())
                .postType("BOAST")
                .title(post.getTitle())
                .contents(post.getContents())
                .view(post.getView())
                .commentCount(post.getComments() != null ? post.getComments().size() : 0)
                .likeCount(post.getPostLikeList() != null ? post.getPostLikeList().size() : 0)
                .isCompleted(null)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    /**
     * LostCatPost 엔티티를 DTO로 변환
     */
    public static MyPostDto fromLostCatPost(LostCatPost post) {
        return MyPostDto.builder()
                .postId(post.getId())
                .postType("LOST")
                .title(post.getTitle())
                .contents(post.getContents())
                .view(post.getView())
                .commentCount(post.getComments() != null ? post.getComments().size() : 0)
                .likeCount(null)  // 실종글은 좋아요 없음
                .isCompleted(post.isCompleted())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}