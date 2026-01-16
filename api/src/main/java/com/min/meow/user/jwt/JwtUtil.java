package com.min.meow.user.jwt;


import com.min.meow.global.Token;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// jwt 생성 및 검증
@Component
public class JwtUtil {
    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret}")String secret){
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // userId를 Long 타입으로 반환
    public Long getUserId(String token){
        String subject = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getSubject();
        return Long.valueOf(subject);
    }



    public String getRole(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
    }

    public Token getTokenCategory(String token){
        String tokenString = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("token", String.class);
        return Token.valueOf(tokenString);
    }

    public Boolean isExpired(String token){

        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getExpiration().before(new Date());
    }

    // Access Token 생성 (subject: userId, payload: role)
    public String createAccessToken(Long userId, Token token, String role, Long expiredMs){
        return Jwts.builder()
                .claims()
                .subject(String.valueOf(userId))
                .add("token", token.name())
                .add("role", role)
                .and()
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(secretKey)
                .compact();
    }

    // Refresh Token 생성 (subject: userId)
    public String createRefreshToken(Long userId, Token token, Long expiredMs){
        return Jwts.builder()
                .claims()
                .subject(String.valueOf(userId))
                .add("token", token.name())
                .and()
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(secretKey)
                .compact();
    }
}
