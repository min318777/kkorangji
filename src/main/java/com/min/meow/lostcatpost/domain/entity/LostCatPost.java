package com.min.meow.lostcatpost.domain.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LostCatPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lostCatPostId;

    @Column(nullable = false)
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

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


}
