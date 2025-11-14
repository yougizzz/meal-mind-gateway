package com.mealmind.gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Component
public class RateLimitFilter implements GatewayFilter {

    @Autowired
    private StringRedisTemplate redis;

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId == null) {
            userId = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }

        String key = "rate:" + userId;
        long count = redis.opsForValue().increment(key);

        if (count == 1) {
            redis.expire(key, 60, TimeUnit.SECONDS);
        }

        if (count > 15) {
            return tooManyRequests(exchange, 15 - count + 1);
        }

        exchange.getResponse().getHeaders()
                .add("X-RateLimit-Remaining", String.valueOf(15 - count));
        return chain.filter(exchange);
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange, long retryAfter) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set("Retry-After", String.valueOf(retryAfter));
        String json = """
                {"error": "Rate limit exceeded", "retryAfter": %d}
                """.formatted(retryAfter);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(json.getBytes())));
    }

}
