package com.min.meow.user.domain;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto {

    private String loginId;
    private String name;
    private String role;
}
