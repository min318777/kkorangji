package com.min.meow.global;


import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class ResponseDto<T> {

    private boolean success;
    private String message;
    private T data;
}
