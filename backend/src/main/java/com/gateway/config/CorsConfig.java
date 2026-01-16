package com.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // 1. Allow Credentials (cookies/auth headers)
        config.setAllowCredentials(true);
        
        // 2. Allow Your Frontend Origins
        config.setAllowedOrigins(Arrays.asList(
            "http://localhost",       // Docker default port 80
            "http://localhost:80",    // Explicit port 80
            "http://localhost:3000",  // React default
            "http://localhost:3001",  // React alternative
            "http://localhost:5173",  // Vite default
            "http://127.0.0.1"        // IP localhost
        ));
        
        // 3. Allow All Headers and Methods
        config.setAllowedHeaders(Arrays.asList("Origin", "Content-Type", "Accept", "Authorization", "X-Api-Key", "X-Api-Secret", "X-Requested-With", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        // 4. Apply to ALL endpoints
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}