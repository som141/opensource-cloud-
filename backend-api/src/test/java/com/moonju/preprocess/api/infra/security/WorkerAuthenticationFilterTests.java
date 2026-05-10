package com.moonju.preprocess.api.infra.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class WorkerAuthenticationFilterTests {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesInternalRequestWithWorkerToken() throws ServletException, IOException {
        WorkerAuthenticationFilter filter = filter("secret-token");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/v1/jobs/1/items/10/started");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(WorkerAuthenticationFilter.WORKER_TOKEN_HEADER, "secret-token");

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
            .extracting("authority")
            .containsExactly("ROLE_WORKER");
    }

    @Test
    void rejectsInternalRequestWithoutWorkerToken() throws ServletException, IOException {
        WorkerAuthenticationFilter filter = filter("secret-token");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/v1/jobs/1/items/10/started");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("WORKER401");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private WorkerAuthenticationFilter filter(String token) {
        WorkerInternalProperties properties = new WorkerInternalProperties();
        properties.setInternalToken(token);
        return new WorkerAuthenticationFilter(properties);
    }
}
