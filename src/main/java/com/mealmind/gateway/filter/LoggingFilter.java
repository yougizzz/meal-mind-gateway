package com.mealmind.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Component
@Slf4j
public class LoggingFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        var request = exchange.getRequest();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            var response = exchange.getResponse();

            String clientIp = getClientIp(request);
            String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
            String uri = request.getURI().toString();
            
            var statusCode = response.getStatusCode();
            int statusCodeValue = statusCode != null ? statusCode.value() : 200;

            log.info("method: {} - uri: {} - status: {} - response time: {}ms - ip: {}", method, uri, statusCodeValue, duration, clientIp);
        }));
    }

    private String getClientIp(ServerHttpRequest request) {
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "unknown";
    }
}
