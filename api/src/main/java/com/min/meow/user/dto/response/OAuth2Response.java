package com.min.meow.user.dto.response;

public interface OAuth2Response {

    String getProvider();

    String getProviderId();

    String getEmail();

    String getName();
}
