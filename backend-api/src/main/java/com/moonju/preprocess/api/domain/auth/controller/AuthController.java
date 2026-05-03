package com.moonju.preprocess.api.domain.auth.controller;

import com.moonju.preprocess.api.domain.auth.dto.AuthMeResponse;
import com.moonju.preprocess.api.domain.auth.dto.LogoutResponse;
import com.moonju.preprocess.api.domain.auth.dto.TokenRefreshResponse;
import com.moonju.preprocess.api.domain.auth.service.AuthService;
import com.moonju.preprocess.api.domain.auth.service.RefreshTokenCookieService;
import com.moonju.preprocess.api.global.response.ApiResponse;
import com.moonju.preprocess.api.global.support.CurrentUser;
import com.moonju.preprocess.api.infra.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Google OAuth login, current user, refresh token, and logout APIs")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    public AuthController(AuthService authService, RefreshTokenCookieService refreshTokenCookieService) {
        this.authService = authService;
        this.refreshTokenCookieService = refreshTokenCookieService;
    }

    @GetMapping("/me")
    @Operation(summary = "Read current user", security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    public ApiResponse<AuthMeResponse> me(@CurrentUser Long currentUserId) {
        return ApiResponse.success(authService.me(currentUserId));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token with HttpOnly refresh cookie")
    public ApiResponse<TokenRefreshResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = refreshTokenCookieService.resolveRefreshToken(request);
        TokenRefreshResponse tokenResponse = authService.refresh(refreshToken);
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            refreshTokenCookieService.createRefreshCookie(tokenResponse.refreshToken()).toString()
        );
        return ApiResponse.success(tokenResponse.withoutRefreshToken());
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke refresh token")
    public ApiResponse<LogoutResponse> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = refreshTokenCookieService.resolveRefreshToken(request);
        authService.logout(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookieService.expireRefreshCookie().toString());
        return ApiResponse.success(new LogoutResponse(true));
    }
}
