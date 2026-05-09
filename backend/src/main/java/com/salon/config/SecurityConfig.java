package com.salon.config;

import com.salon.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${cors.allowed-origin:http://localhost:4200}")
    private String allowedOrigin;

    @Bean
    @SuppressWarnings("deprecation")
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(List.of(allowedOrigin));
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        corsConfig.setAllowedHeaders(List.of("*"));
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // JWT filter will be added in task 5
            .authorizeHttpRequests(auth -> auth
                // Public routes
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/professionals/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/professionals/*/profile").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/professionals/search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/professionals/*/portfolio/public").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/policies/city/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/files/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/services").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/services/list").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                // Professional role
                .requestMatchers(HttpMethod.PUT, "/api/professionals/*/profile").hasRole("PROFESSIONAL")
                // Customer role
                .requestMatchers("/api/customers/**").hasRole("CUSTOMER")
                .requestMatchers("/api/v1/customers/**").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/api/appointments/**").hasRole("CUSTOMER")
                .requestMatchers("/api/appointments").hasAnyRole("CUSTOMER", "SALON_OWNER", "PROFESSIONAL")
                .requestMatchers("/api/appointments/**").hasAnyRole("CUSTOMER", "SALON_OWNER", "PROFESSIONAL")
                .requestMatchers("/api/payments/**").hasRole("CUSTOMER")
                .requestMatchers("/api/reviews/**").hasRole("CUSTOMER")
                // Salon owner role
                .requestMatchers("/api/owners/**").hasRole("SALON_OWNER")
                .requestMatchers("/api/v1/owners/**").hasRole("SALON_OWNER")
                // Professional role
                .requestMatchers("/api/v1/professionals/**").hasAnyRole("PROFESSIONAL", "CUSTOMER", "SALON_OWNER", "ADMIN")
                // Admin role
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/complaints").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.GET, "/api/v1/policy/latest").authenticated()
                .requestMatchers("/api/v1/owners/*/policies/**").authenticated()
                .requestMatchers("/api/v1/owners/*/promotions").authenticated()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
