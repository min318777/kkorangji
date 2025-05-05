package com.min.meow.user.jwt;


import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class JwtProperties {
    private String secret;
    private Long expirationMs;
    private String tokenPrefix;
    private String headerString;
}
