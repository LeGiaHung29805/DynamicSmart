package com.dynamicmart.api_gateway.config;

import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@Configuration
public class HttpClientConfig {

    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        return new JdkClientHttpRequestFactory(httpClient);
    }
}
