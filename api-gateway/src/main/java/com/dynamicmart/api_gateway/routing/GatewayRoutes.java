package com.dynamicmart.api_gateway.routing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

@Configuration
public class GatewayRoutes {
    @Bean RouterFunction<ServerResponse> identityRoute(@Value("${app.routes.identity-service-url}") String target) {
        return route("identity-service").route(path("/api/v1/auth/**")
                .or(path("/api/v1/profile/**"))
                .or(path("/api/v1/addresses/**"))
                .or(path("/api/v1/admin/users/**")), http()).before(uri(target)).build();
    }
    @Bean RouterFunction<ServerResponse> catalogRoute(@Value("${app.routes.catalog-service-url}") String target) {
        return route("catalog-service").route(path("/api/v1/catalog/**"), http()).before(uri(target)).build();
    }
    @Bean RouterFunction<ServerResponse> cartRoute(@Value("${app.routes.cart-service-url}") String target) {
        return route("cart-service").route(path("/api/v1/cart/**"), http()).before(uri(target)).build();
    }
    @Bean RouterFunction<ServerResponse> orderRoute(@Value("${app.routes.order-service-url}") String target) {
        return route("order-service").route(path("/api/v1/orders/**").or(path("/api/v1/checkout/**")), http()).before(uri(target)).build();
    }
    @Bean RouterFunction<ServerResponse> paymentRoute(@Value("${app.routes.payment-service-url}") String target) {
        return route("payment-service").route(path("/api/v1/payments/**").or(path("/api/v1/shipping/**")).or(path("/api/v1/locations/**")), http()).before(uri(target)).build();
    }
    @Bean RouterFunction<ServerResponse> engagementRoute(@Value("${app.routes.engagement-service-url}") String target) {
        return route("engagement-service").route(path("/api/v1/reviews/**").or(path("/api/v1/reports/**")), http()).before(uri(target)).build();
    }
}
