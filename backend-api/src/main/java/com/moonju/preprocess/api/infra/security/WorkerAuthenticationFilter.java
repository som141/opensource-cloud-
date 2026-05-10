package com.moonju.preprocess.api.infra.security;

import com.moonju.preprocess.api.global.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class WorkerAuthenticationFilter extends OncePerRequestFilter {

    public static final String WORKER_TOKEN_HEADER = "X-Worker-Token";

    private final WorkerInternalProperties properties;

    public WorkerAuthenticationFilter(WorkerInternalProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String workerToken = request.getHeader(WORKER_TOKEN_HEADER);
        if (!isValid(workerToken)) {
            writeUnauthorized(response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            "worker",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_WORKER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private boolean isValid(String workerToken) {
        return workerToken != null && workerToken.equals(properties.getInternalToken());
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        ErrorCode errorCode = ErrorCode.WORKER_UNAUTHORIZED;
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
            "{\"isSuccess\":false,\"code\":\"" + errorCode.getCode() + "\",\"message\":\""
                + errorCode.getMessage() + "\"}"
        );
    }
}
