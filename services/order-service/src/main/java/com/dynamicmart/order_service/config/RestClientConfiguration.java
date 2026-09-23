package com.dynamicmart.order_service.config;

import com.dynamicmart.order_service.client.HttpPaymentClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfiguration {
    @Bean
    @Qualifier("paymentRestClient")
    RestClient paymentRestClient(RestClient.Builder builder, OrderClientProperties properties) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        return builder
                .baseUrl(properties.paymentBaseUrl())
                .defaultHeader(HttpPaymentClient.INTERNAL_API_KEY_HEADER, properties.internalApiKey())
                .requestFactory(requestFactory)
                .build();
    }
}
