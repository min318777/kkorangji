package com.min.meow.user.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_token", indexes = {
    // 토큰 조회/검증: WHERE refresh_token = ? (매 API 요청마다 사용)
    @Index(name = "idx_refresh_token_token", columnList = "refresh_token"),
    // 로그아웃 시 삭제: WHERE login_id = ?
    @Index(name = "idx_refresh_token_login_id", columnList = "login_id")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String loginId;
    private Long userId;
    @Column(length = 512)
    private String refreshToken;
    private LocalDateTime expiration;

}
