package com.dynamicmart.api_gateway.routing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.function.ServerRequest;

class GatewayRoutesTest {
    private final GatewayRoutes routes = new GatewayRoutes();

    @Test
    void identityOwnsProfileAddressAndAdminUserRoutes() {
        var identity = routes.identityRoute("http://identity.test");
        var cart = routes.cartRoute("http://cart.test");

        for (String path : new String[]{"/api/v1/auth/login", "/api/v1/profile/me",
                "/api/v1/addresses", "/api/v1/admin/users/1"}) {
            assertThat(identity.route(request(path))).as(path).isPresent();
            assertThat(cart.route(request(path))).as(path).isEmpty();
        }
        assertThat(cart.route(request("/api/v1/cart/items"))).isPresent();
        assertThat(identity.route(request("/api/v1/cart/items"))).isEmpty();
    }

    private ServerRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), path);
        request.setRequestURI(path);
        return ServerRequest.create(request, java.util.List.of());
    }
}
