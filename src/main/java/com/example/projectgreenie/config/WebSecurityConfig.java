package com.example.projectgreenie.config;

import com.example.projectgreenie.security.JwtAuthenticationFilter;
import com.example.projectgreenie.security.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class WebSecurityConfig {

    private final JwtUtil jwtUtil;

    public WebSecurityConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Publicly accessible endpoints (No authentication required)
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/admin/login",
                                "/api/posts/create", // Allow creating posts without authentication
                                "/api/posts/all",
                                "/api/posts/{postId}/like",
                                "/api/users/{id}",
                                "/api/users/{userId}/points",
                                "/api/order/apply-points",
                                "/api/order/place",  // Added this new endpoint
                                "/api/users/all",
                                "/api/products/**",
                                "/api/cart/**",
                                "/shop/**",

                                "/api/posts/{postId}/comments/create",

                                // Challenge endpoints
                                "/api/challenges/all",
                                "/api/challenges/{challengeId}",
                                "/api/leaderboard",  // Added this new endpoint
                                "/api/proof/submit",
                                "/api/proof/all",
                                "/api/proof/{id}"
                        ).permitAll()

                        // Protected Endpoints (Require Authentication)
                        .requestMatchers("/api/challenges/create").authenticated()
                        .requestMatchers("/api/proof/").authenticated()

                        // Admin-Only Endpoints (Requires ADMIN role)
                        .requestMatchers("/admin/**").hasAuthority("ADMIN") // Secure admin routes

                        // Feed Post
                        .requestMatchers("/api/posts/create").permitAll() // Allow creating posts without authentication
                        .requestMatchers("/api/posts/{postId}/like").permitAll()
                        .requestMatchers("/api/posts/{postId}/comments/create").permitAll()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://test.greenie.dizzpy.dev", "http://localhost:5173")); // Merged both versions
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Authorization")); // Allow frontend to read the token
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
