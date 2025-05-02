package com.min.meow.boastcatpost.entity;


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

}
