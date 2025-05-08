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
public class JwtUtils {
    private final SecretKey secretKey;

    public JwtUtils(@Value("${jwt.secret}")String secret){
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }


    public String getLoginId(String token){

        return Jwts.parser().verifyWith(secretKey).build().parseEncryptedClaims(token).getPayload().get("loginId", String.class);
    }


    public String getRole(String token){

        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
    }

    public Token getTokenCategory(String token){

        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("tokenCategory", Token.class);
    }

    public Boolean isExpiration(String token){

        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getExpiration().before(new Date());
    }

    public String createJwt(Token token, String loginId, String role, Long expiredMs){

        return Jwts.builder()
                .claim("tokenCategory", token)
                .claim("loginId", loginId)
                .claim("role", role)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(secretKey)
                .compact();
    }

}
