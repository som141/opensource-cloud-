package com.moonju.preprocess.api.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moonju.preprocess.api.domain.auth.exception.InvalidTokenException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RefreshTokenCookieServiceTests {

    private final RefreshTokenCookieService service = new RefreshTokenCookieService(
        "refresh_token",
        false,
        "Lax",
        1209600L
    );

    @Test
    void createsHttpOnlyRefreshCookie() {
        String cookie = service.createRefreshCookie("refresh-token").toString();

        assertThat(cookie).contains("refresh_token=refresh-token");
        assertThat(cookie).contains("HttpOnly");
        assertThat(cookie).contains("SameSite=Lax");
    }

    @Test
    void resolvesRefreshTokenFromCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refresh_token", "refresh-token"));

        assertThat(service.resolveRefreshToken(request)).isEqualTo("refresh-token");
    }

    @Test
    void rejectsMissingRefreshCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> service.resolveRefreshToken(request))
            .isInstanceOf(InvalidTokenException.class)
            .hasMessage("Refresh token cookie is missing.");
    }
}
