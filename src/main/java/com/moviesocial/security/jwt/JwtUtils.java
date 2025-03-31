package com.moviesocial.security.jwt;

import com.moviesocial.security.services.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject((userPrincipal.getUsername()))
                .claim("email", userPrincipal.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public String getEmailFromJwtToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody();
        return claims.get("email", String.class) != null 
               ? claims.get("email", String.class) 
               : claims.getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            logger.info("JWT 토큰 검증 시작");
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(authToken)
                    .getBody();
            
            Date expiration = claims.getExpiration();
            Date now = new Date();
            long remainingTime = (expiration.getTime() - now.getTime()) / 1000; // 초 단위
            
            logger.info("JWT 토큰 검증 성공 - 사용자: {}, 만료시간: {}, 남은시간: {}초", 
                    claims.getSubject(), 
                    expiration, 
                    remainingTime);
            
            return true;
        } catch (SecurityException e) {
            logger.error("JWT 서명이 유효하지 않음: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("JWT 토큰 형식이 잘못됨: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT 토큰이 만료됨. 만료시간: {}, 현재시간: {}", 
                    e.getClaims().getExpiration(), 
                    new Date());
        } catch (UnsupportedJwtException e) {
            logger.error("지원되지 않는 JWT 토큰: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims 문자열이 비어있음: {}", e.getMessage());
        }

        logger.error("JWT 토큰 검증 실패");
        return false;
    }
}