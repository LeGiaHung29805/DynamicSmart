package com.dynamicmart.payment_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PaymentServiceApplicationTests {

	@Test
	void contextLoads() {
		assertNotNull(PaymentServiceApplication.class.getAnnotation(SpringBootApplication.class));
	}

}
