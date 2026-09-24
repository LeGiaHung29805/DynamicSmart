package com.dynamicmart.order_service;

import com.dynamicmart.order_service.repository.CheckoutSessionItemRepository;
import com.dynamicmart.order_service.repository.CheckoutSessionRepository;
import com.dynamicmart.order_service.repository.CheckoutSessionVoucherRepository;
import com.dynamicmart.order_service.repository.CheckoutShippingQuoteRepository;
import com.dynamicmart.order_service.repository.OrderSagaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.mockito.Mockito;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude="
				+ "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
				+ "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
				+ "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
				+ "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
		"app.security.jwt.hmac-secret-base64=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
		"app.clients.internal-api-key=test-internal-key"
})
@Import(OrderServiceApplicationTests.TestDoubles.class)
class OrderServiceApplicationTests {

	@Test
	void contextLoads() {
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestDoubles {
		@Bean
		CheckoutSessionRepository checkoutSessionRepository() {
			return Mockito.mock(CheckoutSessionRepository.class);
		}

		@Bean
		CheckoutSessionItemRepository checkoutSessionItemRepository() {
			return Mockito.mock(CheckoutSessionItemRepository.class);
		}

		@Bean
		CheckoutSessionVoucherRepository checkoutSessionVoucherRepository() {
			return Mockito.mock(CheckoutSessionVoucherRepository.class);
		}

		@Bean
		CheckoutShippingQuoteRepository checkoutShippingQuoteRepository() {
			return Mockito.mock(CheckoutShippingQuoteRepository.class);
		}

		@Bean
		OrderSagaRepository orderSagaRepository() {
			return Mockito.mock(OrderSagaRepository.class);
		}
	}
}
