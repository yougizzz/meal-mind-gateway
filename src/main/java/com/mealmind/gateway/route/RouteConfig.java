package com.mealmind.gateway.route;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mealmind.gateway.config.JwtValidationFilter;
import com.mealmind.gateway.config.RateLimitFilter;

@Configuration
public class RouteConfig {
        private static final Logger log = LoggerFactory.getLogger(RouteConfig.class);

        @Bean
        public RouteLocator routeLocator(
                        RouteLocatorBuilder builder,
                        JwtValidationFilter jwtFilter,
                        RateLimitFilter rateLimitFilter) {
                log.info("Configuring gateway routes...");
                log.info("Auth route: /api/auth/** -> http://localhost:8082/auth/**");
                
                return builder.routes()
                                .route("auth", r -> r.path("/api/auth/**")
                                                .filters(f -> f.stripPrefix(1))
                                                .uri("http://localhost:8082"))
                                .build();
        }
}
