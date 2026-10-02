package com.dynamicmart.order_service.messaging;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dynamicmart.order_service.client.CartOrderConfirmationGateway;
import com.dynamicmart.order_service.config.OutboxProperties;
import com.dynamicmart.order_service.entity.OutboxEvent;
import com.dynamicmart.order_service.entity.OutboxEventStatus;
import com.dynamicmart.order_service.repository.OutboxEventRepository;
import com.dynamicmart.order_service.service.OrderOutboxPublisher;
import com.dynamicmart.order_service.service.OrderPaymentEventHandler;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.EnableTestBinder;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
        classes = PaymentMessagingIntegrationTests.TestApplication.class,
        properties = {
                "spring.cloud.function.definition=paymentEvents",
                "spring.cloud.stream.bindings.paymentEvents-in-0.destination=dynamicmart.events",
                "spring.cloud.stream.bindings.paymentEvents-in-0.group=order-service",
                "spring.cloud.stream.bindings.paymentEvents-in-0.content-type=application/json",
                "spring.cloud.stream.bindings.paymentEvents-in-0.consumer.max-attempts=5",
                "spring.cloud.stream.bindings.paymentEvents-in-0.consumer.back-off-initial-interval=1",
                "spring.cloud.stream.bindings.paymentEvents-in-0.consumer.back-off-max-interval=1",
                "spring.cloud.stream.bindings.paymentEvents-in-0.consumer.back-off-multiplier=1",
                "spring.cloud.stream.bindings.orderEvents-out-0.destination=dynamicmart.events",
                "spring.cloud.stream.bindings.orderEvents-out-0.content-type=application/json",
                "spring.cloud.stream.bindings.paymentDue-out-0.destination=payment.due",
                "spring.cloud.stream.bindings.paymentDue-out-0.content-type=application/json",
                "logging.level.org.springframework.integration.handler.LoggingHandler=OFF",
                "logging.level.org.springframework.cloud.stream.binder.test.TestChannelBinder=OFF",
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
        })
class PaymentMessagingIntegrationTests {
    private static final UUID EVENT_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID PAYMENT_ID = UUID.fromString("50000000-0000-0000-0000-000000000002");
    private static final UUID ORDER_ID = UUID.fromString("50000000-0000-0000-0000-000000000003");
    private static final UUID CORRELATION_ID = UUID.fromString("50000000-0000-0000-0000-000000000004");
    private static final Instant NOW = Instant.parse("2026-10-02T03:00:00Z");

    @Autowired private InputDestination input;
    @Autowired private OutputDestination output;
    @Autowired private OrderPaymentEventHandler handler;
    @Autowired private OutboxEventRepository events;
    @Autowired private OrderOutboxPublisher publisher;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void resetDoublesAndDestinations() {
        Mockito.reset(handler, events);
        output.clear();
    }

    @Test
    void jsonPaymentEventIsConvertedAndDeliveredThroughConfiguredBinding() {
        sendPaymentEvent();

        verify(handler).handle(argThat(event ->
                EVENT_ID.equals(event.eventId())
                        && "PaymentSucceeded".equals(event.eventType())
                        && PAYMENT_ID.equals(event.payload().paymentId())
                        && ORDER_ID.equals(event.payload().orderId())
                        && event.payload().amountVnd() == 120_000));
    }

    @Test
    void failedPaymentHandlerIsRetriedUsingConsumerPolicy() {
        Mockito.doThrow(new IllegalStateException("temporary database failure"))
                .when(handler).handle(any(PaymentEventEnvelope.class));

        try {
            sendPaymentEvent();
        } catch (RuntimeException expectedAfterRetries) {
            // The test binder may propagate exhaustion; Rabbit republish-to-DLQ handles it in production.
        }

        verify(handler, times(5)).handle(any(PaymentEventEnvelope.class));
    }

    @Test
    void paymentDueOutboxEventReachesDedicatedDestinationAsJson() {
        OutboxEvent event = OutboxEvent.pending(
                EVENT_ID, "ORDER", ORDER_ID, "PaymentDue", 1,
                "{\"orderId\":\"" + ORDER_ID + "\"}", CORRELATION_ID, NOW.minusSeconds(1));
        when(events.findById(EVENT_ID)).thenReturn(Optional.of(event));

        publisher.publishOne(EVENT_ID);

        Message<byte[]> message = output.receive(1_000, "payment.due");
        assertNotNull(message);
        var json = objectMapper.readTree(message.getPayload());
        assertEquals(EVENT_ID.toString(), json.get("eventId").stringValue());
        assertEquals(ORDER_ID.toString(), json.get("orderId").stringValue());
        assertEquals(CORRELATION_ID.toString(), json.get("correlationId").stringValue());
        assertEquals(OutboxEventStatus.PUBLISHED, event.getStatus());
        assertEquals(NOW, event.getPublishedAt());
    }

    @Test
    void regularOrderOutboxEventUsesVersionedSharedEnvelope() {
        OutboxEvent event = OutboxEvent.pending(
                EVENT_ID, "ORDER", ORDER_ID, "OrderCompleted", 1,
                "{\"orderId\":\"" + ORDER_ID + "\",\"status\":\"COMPLETED\"}",
                CORRELATION_ID, NOW.minusSeconds(1));
        when(events.findById(EVENT_ID)).thenReturn(Optional.of(event));

        publisher.publishOne(EVENT_ID);

        verify(handler).handle(argThat(envelope ->
                EVENT_ID.equals(envelope.eventId())
                        && "OrderCompleted".equals(envelope.eventType())
                        && envelope.eventVersion() == 1
                        && "order-service".equals(envelope.producer())
                        && ORDER_ID.equals(envelope.aggregateId())
                        && CORRELATION_ID.equals(envelope.correlationId())));
        assertEquals(OutboxEventStatus.PUBLISHED, event.getStatus());
    }

    private void sendPaymentEvent() {
        String json = """
                {
                  "eventId": "%s",
                  "eventType": "PaymentSucceeded",
                  "eventVersion": 1,
                  "producer": "payment-service",
                  "aggregateId": "%s",
                  "occurredAt": "%s",
                  "correlationId": "%s",
                  "payload": {
                    "paymentId": "%s",
                    "orderId": "%s",
                    "amountVnd": 120000,
                    "timing": "PREPAID",
                    "method": "VNPAY",
                    "status": "SUCCEEDED"
                  }
                }
                """.formatted(EVENT_ID, PAYMENT_ID, NOW, CORRELATION_ID, PAYMENT_ID, ORDER_ID);
        input.send(MessageBuilder.withPayload(json.getBytes(UTF_8))
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                .build(), "dynamicmart.events");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableTestBinder
    @Import({PaymentEventConsumerConfiguration.class, OrderOutboxPublisher.class})
    static class TestApplication {
        @Bean
        OrderPaymentEventHandler orderPaymentEventHandler() {
            return Mockito.mock(OrderPaymentEventHandler.class);
        }

        @Bean
        OutboxEventRepository outboxEventRepository() {
            return Mockito.mock(OutboxEventRepository.class);
        }

        @Bean
        CartOrderConfirmationGateway cartOrderConfirmationGateway() {
            return Mockito.mock(CartOrderConfirmationGateway.class);
        }

        @Bean
        OutboxProperties outboxProperties() {
            return new OutboxProperties(Duration.ofSeconds(1), 100);
        }

        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
