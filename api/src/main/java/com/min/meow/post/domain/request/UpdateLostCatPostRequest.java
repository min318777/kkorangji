package com.min.meow.post.domain.request;


import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLostCatPostRequest {

    private String title;

    private String content;

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
}
