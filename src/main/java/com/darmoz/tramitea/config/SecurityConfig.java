package com.darmoz.tramitea.config;

import com.darmoz.tramitea.security.JwtAuthenticationFilter;
import com.darmoz.tramitea.security.TokenVerifier;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Sin CORS: el navegador nunca llama a esta API. El front (Next.js) la
 * consume desde sus route handlers por la red Docker interna, reenviando el
 * token de la cookie de sesion.
 */
@Configuration
@EnableConfigurationProperties({SessionTokenProperties.class, DarmozAuthProperties.class})
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, TokenVerifier tokenVerifier)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Solo existen en modo local; en modo darmoz responden 404.
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/sign-in").permitAll()
                        // Catalogo, comunidad (lectura) y sugerencias: publicos.
                        .requestMatchers(HttpMethod.GET, "/procedures", "/procedures/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/procedures/suggestions").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(tokenVerifier),
                        UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
