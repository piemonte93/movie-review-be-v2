package com.moviesocial.security.jwt;

import com.moviesocial.model.User;
import com.moviesocial.security.services.UserDetailsImpl;
import com.moviesocial.security.services.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String requestUri = request.getRequestURI();
            String method = request.getMethod();
            logger.info("요청 URI: {}, 메소드: {}", requestUri, method);
            
            String jwt = parseJwt(request);
            if (jwt != null) {
                logger.info("JWT 토큰 파싱 성공");
                if (jwtUtils.validateJwtToken(jwt)) {
                    logger.info("JWT 토큰 검증 성공");
                    String username = jwtUtils.getUserNameFromJwtToken(jwt);
                    logger.info("JWT 토큰에서 사용자명 추출: {}", username);

                    UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(username);
                    User user = userDetails.getUser();
                    
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.info("인증 정보 설정 완료: {}, 권한: {}", username, userDetails.getAuthorities());
                } else {
                    logger.error("JWT 토큰 검증 실패");
                }
            } else {
                if (requestUri.startsWith("/api/movie-reviews")) {
                    logger.error("영화 리뷰 요청이지만 JWT 토큰이 없음");
                } else {
                    logger.debug("JWT 토큰이 없음, 인증이 필요하지 않은 경로일 수 있음");
                }
            }
        } catch (Exception e) {
            logger.error("인증 처리 중 오류 발생: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}