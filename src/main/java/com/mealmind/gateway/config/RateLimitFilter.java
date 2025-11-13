package com.mealmind.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Component
public class RateLimitFilter extends AbstractGatewayFilterFactory<RateLimitFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String REMAINING_HEADER = "X-RateLimit-Remaining";
    private final ReactiveStringRedisTemplate redisTemplate;
    private final Duration window;

    public RateLimitFilter(ReactiveStringRedisTemplate redisTemplate,
                           @Value("${gateway.rate-limit.window-seconds:60}") long windowSeconds) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("burstCapacity", "replenishRate");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String key = buildKey(request, config);
            int limit = config.getBurstCapacity();

            return redisTemplate.opsForValue()
                    .increment(key)
                    .flatMap(currentCount -> ensureTtl(key, currentCount)
                            .thenReturn(currentCount))
                    .flatMap(currentCount -> {
                        if (currentCount > limit) {
                            log.debug("Rate limit exceeded for key {} (limit {}, count {})", key, limit, currentCount);
                            return tooManyRequests(exchange, limit, currentCount);
                        }
                        long remaining = Math.max(0, limit - currentCount);
                        exchange.getResponse().getHeaders().set(REMAINING_HEADER, Long.toString(remaining));
                        return chain.filter(exchange);
                    })
                    .onErrorResume(throwable -> {
                        log.warn("Rate limiting failed for key {}. Allowing request. Cause: {}", key, throwable.getMessage());
                        return chain.filter(exchange);
                    });
        };
    }

    private Mono<Long> ensureTtl(String key, Long currentCount) {
        if (Objects.equals(currentCount, 1L)) {
            return redisTemplate.expire(key, window)
                    .thenReturn(currentCount);
        }
        return Mono.just(currentCount);
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange, int limit, Long currentCount) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().set(REMAINING_HEADER, "0");
        exchange.getResponse().getHeaders().set("Retry-After", Long.toString(window.getSeconds()));
        return exchange.getResponse().setComplete();
    }

    private String buildKey(ServerHttpRequest request, Config config) {
        String clientId = resolveClientId(request, config);
        return "%s:%s".formatted(config.getKeyPrefix(), clientId);
    }

    private String resolveClientId(ServerHttpRequest request, Config config) {
        String headerName = config.getHeaderName();
        String forwarded = request.getHeaders().getFirst(headerName);
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return request.getId();
    }

    public static class Config {
        private int burstCapacity = 60;
        private int replenishRate = 60;
        private String keyPrefix = "gateway:rate";
        private String headerName = "X-Forwarded-For";

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }

        public int getReplenishRate() {
            return replenishRate;
        }

        public void setReplenishRate(int replenishRate) {
            this.replenishRate = replenishRate;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }
    }
}

