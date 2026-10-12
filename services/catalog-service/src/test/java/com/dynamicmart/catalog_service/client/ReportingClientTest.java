package com.dynamicmart.catalog_service.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.dynamicmart.catalog_service.config.CatalogClientProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

class ReportingClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void readsReportingContractDeduplicatesProductsAndSendsInternalKey() throws Exception {
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();
        AtomicReference<String> internalKey = new AtomicReference<>();
        startServer(exchange -> {
            internalKey.set(exchange.getRequestHeaders().getFirst("X-Internal-Api-Key"));
            respond(exchange, HttpStatus.OK.value(), """
                    {"data":[
                      {"productId":"%s","variantId":null,"quantitySold":12,"grossSalesVnd":1200000,"netItemSalesVnd":1100000},
                      {"productId":"%s","variantId":"%s","quantitySold":5,"grossSalesVnd":500000,"netItemSalesVnd":450000},
                      {"productId":"%s","variantId":null,"quantitySold":4,"grossSalesVnd":400000,"netItemSalesVnd":350000}
                    ]}
                    """.formatted(firstProductId, firstProductId, UUID.randomUUID(), secondProductId));
        });
        ReportingClient client = client(500, 500, List.of());

        List<UUID> result = client.getBestSellers(
                LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 13), 10);

        assertThat(result).containsExactly(firstProductId, secondProductId);
        assertThat(internalKey.get()).isEqualTo("test-internal-key");
    }

    @Test
    void fallsBackQuicklyWhenReportingRespondsTooSlowly() throws Exception {
        UUID fallbackId = UUID.randomUUID();
        startServer(exchange -> {
            try {
                Thread.sleep(350);
                respond(exchange, HttpStatus.OK.value(), "{\"data\":[]}");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (IOException ignored) {
                // The client is expected to close the connection after its read timeout.
            }
        });
        ReportingClient client = client(100, 75, List.of(fallbackId));

        long startedAt = System.nanoTime();
        List<UUID> result = client.getBestSellers(LocalDate.now().minusDays(6), LocalDate.now(), 10);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

        assertThat(result).containsExactly(fallbackId);
        assertThat(elapsed).isLessThan(Duration.ofSeconds(2));
    }

    private ReportingClient client(int connectTimeoutMs, int readTimeoutMs,
                                   List<UUID> fallbackIds) {
        CatalogClientProperties properties = new CatalogClientProperties(baseUrl(), connectTimeoutMs,
                readTimeoutMs, fallbackIds);
        return new ReportingClient(properties, "test-internal-key", RestClient.builder());
    }

    private void startServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/reports/products/best-sellers", handler);
        server.start();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
