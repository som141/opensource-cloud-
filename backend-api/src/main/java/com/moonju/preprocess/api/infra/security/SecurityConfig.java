package com.moonju.preprocess.api.infra.security;

import com.moonju.preprocess.api.domain.auth.service.OAuth2LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final WorkerAuthenticationFilter workerAuthenticationFilter;

    public SecurityConfig(
        OAuth2LoginSuccessHandler oauth2LoginSuccessHandler,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        WorkerAuthenticationFilter workerAuthenticationFilter
    ) {
        this.oauth2LoginSuccessHandler = oauth2LoginSuccessHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.workerAuthenticationFilter = workerAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    antMatcher("/"),
                    antMatcher("/error"),
                    antMatcher("/actuator/health"),
                    antMatcher("/actuator/info"),
                    antMatcher("/oauth2/**"),
                    antMatcher("/login/oauth2/**"),
                    antMatcher("/api/v1/auth/refresh"),
                    antMatcher("/api/v1/auth/logout"),
                    antMatcher("/swagger-ui/**"),
                    antMatcher("/v3/api-docs/**")
                ).permitAll()
                .requestMatchers(antMatcher("/internal/**")).hasRole("WORKER")
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oauth2LoginSuccessHandler)
            )
            .addFilterBefore(workerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
