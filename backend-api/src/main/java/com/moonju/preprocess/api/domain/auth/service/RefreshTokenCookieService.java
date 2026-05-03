package com.moonju.preprocess.api.domain.auth.service;

import com.moonju.preprocess.api.domain.auth.exception.InvalidTokenException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieService {

    private final String cookieName;
    private final boolean secure;
    private final String sameSite;
    private final Duration refreshTokenTtl;

    public RefreshTokenCookieService(
        @Value("${app.auth.refresh-cookie.name}") String cookieName,
        @Value("${app.auth.refresh-cookie.secure}") boolean secure,
        @Value("${app.auth.refresh-cookie.same-site}") String sameSite,
        @Value("${app.jwt.refresh-token-expire-seconds}") long refreshTokenExpireSeconds
    ) {
        this.cookieName = cookieName;
        this.secure = secure;
        this.sameSite = sameSite;
        this.refreshTokenTtl = Duration.ofSeconds(refreshTokenExpireSeconds);
    }

    public ResponseCookie createRefreshCookie(String refreshToken) {
        return ResponseCookie.from(cookieName, refreshToken)
            .httpOnly(true)
            .secure(secure)
            .path("/")
            .sameSite(sameSite)
            .maxAge(refreshTokenTtl)
            .build();
    }

    public ResponseCookie expireRefreshCookie() {
        return ResponseCookie.from(cookieName, "")
            .httpOnly(true)
            .secure(secure)
            .path("/")
            .sameSite(sameSite)
            .maxAge(Duration.ZERO)
            .build();
    }

    public String resolveRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new InvalidTokenException("Refresh token cookie is missing.");
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        throw new InvalidTokenException("Refresh token cookie is missing.");
    }
}
