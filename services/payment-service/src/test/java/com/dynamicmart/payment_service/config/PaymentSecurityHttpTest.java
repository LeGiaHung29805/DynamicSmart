package com.dynamicmart.payment_service.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PaymentSecurityTestController.class)
@Import({SecurityConfig.class, InternalApiAuthenticationFilter.class, PaymentSecurityHttpTest.TestBeans.class})
@TestPropertySource(properties = {
        "app.security.jwt.issuer=dynamicmart-identity-service",
        "app.security.jwt.hmac-secret-base64=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="
})
class PaymentSecurityHttpTest {
    private static final String INTERNAL_KEY = "test-internal-key";

    @Autowired private MockMvc mockMvc;

    @Test
    void publicEndpointRemainsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/payments/methods")).andExpect(status().isOk());
    }

    @Test
    void adminEndpointRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/v1/payments")).andExpect(status().isUnauthorized());
    }

    @Test
    void callerSuppliedCustomerHeaderDoesNotAuthenticateRequest() throws Exception {
        mockMvc.perform(get("/api/v1/payments/my-orders/00000000-0000-0000-0000-000000000001")
                        .header("X-Authenticated-User-Id", "00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerJwtCanUseCustomerEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/payments/my-orders/00000000-0000-0000-0000-000000000001")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isOk());
    }

    @Test
    void customerCannotUseAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/payments")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUseAdminEndpoints() throws Exception {
        var admin = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
        mockMvc.perform(get("/api/v1/payments").with(admin)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/locations/sync").with(admin)).andExpect(status().isOk());
    }

    @Test
    void internalEndpointRequiresServiceKey() throws Exception {
        mockMvc.perform(post("/api/v1/payments/order-context"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/payments/order-context")
                        .header(InternalApiAuthenticationFilter.HEADER, INTERNAL_KEY))
                .andExpect(status().isOk());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean InternalApiProperties internalApiProperties() {
            return new InternalApiProperties(INTERNAL_KEY);
        }
    }
}