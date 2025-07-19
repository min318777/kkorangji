package com.min.meow.post.search.domain.request;


import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostSearchRequest {

    private Long id;
    private String title;
    private String contents;
    private int view;
    private Long userId;
    //private int CategoryType;
    //private CategoryDto.SortStatus sortStatus;
}
