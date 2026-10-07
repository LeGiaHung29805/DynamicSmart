package com.dynamicmart.catalog_service.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CatalogSecurityTestController.class)
@Import({SecurityConfig.class, CatalogSecurityHttpTest.TestBeans.class})
@ActiveProfiles("catalog-security-test")
class CatalogSecurityHttpTest {
    @Autowired private MockMvc mockMvc;

    @Test
    void healthAndPublicCatalogAreAnonymous() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/catalog/products")).andExpect(status().isOk());
    }

    @Test
    void adminCatalogRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/admin/products"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/catalog/admin/products")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/catalog/admin/products")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void internalRouteIsLeftToInternalApiVerifierAndOtherRoutesNeedAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/internal/ping"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/catalog/private"))
                .andExpect(status().isUnauthorized());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean
        CatalogJwtProperties catalogJwtProperties() {
            String secret = Base64.getEncoder().encodeToString(
                    "01234567890123456789012345678901".getBytes());
            return new CatalogJwtProperties("dynamicmart-identity-service", secret);
        }
    }
}
