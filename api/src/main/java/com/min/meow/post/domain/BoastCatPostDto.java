package com.min.meow.post.domain;


import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BoastCatPostDto {

    private Long id;
    private Long userId;
    private String title;
    private String contents;
    private int view;
    private String catImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
