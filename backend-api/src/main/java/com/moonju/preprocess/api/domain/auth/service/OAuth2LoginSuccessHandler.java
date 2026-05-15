package com.moonju.preprocess.api.domain.auth.service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    private final AuthService authService;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final String successRedirectUri;

    public OAuth2LoginSuccessHandler(
        AuthService authService,
        RefreshTokenCookieService refreshTokenCookieService,
        @Value("${app.oauth2.success-redirect-uri}") String successRedirectUri
    ) {
        this.authService = authService;
        this.refreshTokenCookieService = refreshTokenCookieService;
        this.successRedirectUri = successRedirectUri;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = oauthToken.getPrincipal();
        IssuedTokenPair tokenPair = authService.loginWithOAuth2(
            oauthToken.getAuthorizedClientRegistrationId(),
            oauthUser.getAttributes()
        );

        response.addHeader(
            HttpHeaders.SET_COOKIE,
            refreshTokenCookieService.createRefreshCookie(tokenPair.refreshToken()).toString()
        );
        String redirectUri = UriComponentsBuilder.fromUriString(successRedirectUri)
            .queryParam("login", "success")
            .build()
            .toUriString();
        redirectStrategy.sendRedirect(request, response, redirectUri);
    }
}
