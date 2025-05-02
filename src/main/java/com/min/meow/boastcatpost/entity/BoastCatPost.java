package com.min.meow.boastcatpost.entity;


import com.min.meow.boastcatpost.domain.request.CreateBoastCatPostRequest;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BoastCatPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long boastCatPostId;

    private String title;

    private String content;

    private String catImageUrl;

    //private List<LostCatPostCommentDto> lostCatPostCommentDtos;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }
    @PreUpdate
    public void preUpdate(){
        this.updatedAt = LocalDateTime.now();
    }


    public static BoastCatPost convertToEntity(CreateBoastCatPostRequest createBoastCatPostRequest){

        return BoastCatPost.builder()
                .title(createBoastCatPostRequest.getTitle())
                .content(createBoastCatPostRequest.getContent())
                .catImageUrl(createBoastCatPostRequest.getCatImageUrl())
                .build();
    }
}
