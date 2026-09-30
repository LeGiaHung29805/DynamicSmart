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
        return client(builder, properties, properties.paymentBaseUrl(), HttpPaymentClient.INTERNAL_API_KEY_HEADER);
    }

    @Bean @Qualifier("identityRestClient")
    RestClient identityRestClient(RestClient.Builder builder, OrderClientProperties properties) {
        return client(builder, properties, properties.identityBaseUrl(), HttpPaymentClient.INTERNAL_API_KEY_HEADER);
    }
    @Bean @Qualifier("cartRestClient")
    RestClient cartRestClient(RestClient.Builder builder, OrderClientProperties properties) {
        return client(builder, properties, properties.cartBaseUrl(), HttpPaymentClient.INTERNAL_API_KEY_HEADER);
    }
    @Bean @Qualifier("catalogRestClient")
    RestClient catalogRestClient(RestClient.Builder builder, OrderClientProperties properties) {
        return client(builder, properties, properties.catalogBaseUrl(), HttpPaymentClient.INTERNAL_API_KEY_HEADER);
    }

    private RestClient client(RestClient.Builder builder, OrderClientProperties properties, java.net.URI baseUrl, String header) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        return builder
                .baseUrl(baseUrl)
                .defaultHeader(header, properties.internalApiKey())
                .requestFactory(requestFactory)
                .build();
    }
}
