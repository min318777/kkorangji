package com.min.meow.lostcatpost.domain.request;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UpdateLostCatPostRequest {

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
}
