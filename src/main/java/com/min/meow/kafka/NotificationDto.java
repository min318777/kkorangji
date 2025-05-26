package com.min.meow.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationMessage {

    private Long commentId;
    private Long postId;
    private String message;
    private String receiverLoginId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
