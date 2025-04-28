package com.min.meow.lostcatpost.domain;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class LostCatPostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lostCatPostId;

    @Column(nullable = false)
    private String title;
    private String content;

    private String catName;
    private Integer catAge;
    private Integer catWeight;
    private String catImageUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;



}
