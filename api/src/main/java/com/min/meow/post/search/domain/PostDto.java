package com.min.meow.post.search.domain;


import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PostDto {

    private Long id;
    private String title;
    private String contents;
    private int view;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
