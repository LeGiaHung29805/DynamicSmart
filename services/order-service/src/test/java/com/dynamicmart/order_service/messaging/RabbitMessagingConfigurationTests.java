package com.dynamicmart.order_service.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.cloud.stream.binder.rabbit.properties.RabbitConsumerProperties;
import org.springframework.cloud.stream.binder.rabbit.properties.RabbitExtendedBindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.core.io.ClassPathResource;

class RabbitMessagingConfigurationTests {
    @Test
    void paymentConsumerDlqPropertiesBindToCurrentRabbitBinderContract() throws IOException {
        var yaml = new YamlPropertySourceLoader().load(
                "order-service-application", new ClassPathResource("application.yaml"));
        var sources = new ArrayList<org.springframework.boot.context.properties.source.ConfigurationPropertySource>();
        yaml.forEach(source -> ConfigurationPropertySources.from(source).forEach(sources::add));
        RabbitExtendedBindingProperties properties = new Binder(sources)
                .bind("spring.cloud.stream.rabbit", Bindable.of(RabbitExtendedBindingProperties.class))
                .orElseThrow(AssertionError::new);
        BindingServiceProperties bindings = new Binder(sources)
                .bind("spring.cloud.stream", Bindable.of(BindingServiceProperties.class))
                .orElseThrow(AssertionError::new);

        RabbitConsumerProperties consumer = properties.getExtendedConsumerProperties("paymentEvents-in-0");
        var paymentBinding = bindings.getBindingProperties("paymentEvents-in-0");

        assertEquals("dynamicmart.events", paymentBinding.getDestination());
        assertEquals("order-service", paymentBinding.getGroup());
        assertEquals(5, paymentBinding.getConsumer().getMaxAttempts());
        assertTrue(consumer.isAutoBindDlq());
        assertTrue(consumer.isRepublishToDlq());
        assertFalse(consumer.isRequeueRejected());
        assertEquals("dynamicmart.events.dlx", consumer.getDeadLetterExchange());
        assertEquals("dynamicmart.events.order-service.dlq", consumer.getDeadLetterQueueName());
        assertEquals("order-service.payment.failed", consumer.getDeadLetterRoutingKey());
    }
}
