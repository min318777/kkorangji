package com.min.meow.user.jwt;


import com.min.meow.global.Role;
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

    public String getUserId(String token){
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getSubject();
    }

    public String getLoginId(String token){
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("loginId", String.class);
    }

    public Role getRole(String token){
        String roleString = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
        return Role.valueOf(roleString);
    }

    public Token getTokenCategory(String token){
        String tokenString = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("token", String.class);
        return Token.valueOf(tokenString);
    }

    public Boolean isExpired(String token){

        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getExpiration().before(new Date());
    }

    public String createAccessToken(Long userId, Token token, String loginId, String role, Long expiredMs){

        return Jwts.builder()
                .claims()
                .subject(String.valueOf(userId))
                .add("token", token.name())
                .add("loginId", loginId)
                .add("role", role)
                .and()
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Long userId, Long expireMs, Token token){
        return Jwts.builder()
                .claims()
                .subject(String.valueOf(userId))
                .add("token", token.name())
                .and()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expireMs))
                .signWith(secretKey)
                .compact();
    }
}
