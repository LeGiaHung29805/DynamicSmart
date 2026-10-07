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

class EngagementReportClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void readsCurrentEngagementContractAndSendsInternalKey() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        AtomicReference<String> internalKey = new AtomicReference<>();
        startServer(exchange -> {
            internalKey.set(exchange.getRequestHeaders().getFirst("X-Internal-Api-Key"));
            respond(exchange, HttpStatus.OK.value(), """
                    {"data":[{
                      "productId":"%s",
                      "variantId":"%s",
                      "quantitySold":12,
                      "grossSalesVnd":1200000,
                      "netItemSalesVnd":1100000
                    }]}
                    """.formatted(productId, variantId));
        });
        EngagementReportClient client = client(500, 500, List.of());

        List<UUID> result = client.getBestSellers(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1), 10);

        assertThat(result).containsExactly(productId);
        assertThat(internalKey.get()).isEqualTo("test-internal-key");
    }

    @Test
    void fallsBackQuicklyWhenEngagementRespondsTooSlowly() throws Exception {
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
        EngagementReportClient client = client(100, 75, List.of(fallbackId));

        long startedAt = System.nanoTime();
        List<UUID> result = client.getBestSellers(LocalDate.now().minusDays(30), LocalDate.now(), 10);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

        assertThat(result).containsExactly(fallbackId);
        assertThat(elapsed).isLessThan(Duration.ofSeconds(2));
    }

    private EngagementReportClient client(int connectTimeoutMs, int readTimeoutMs,
                                          List<UUID> fallbackIds) {
        CatalogClientProperties properties = new CatalogClientProperties(baseUrl(), connectTimeoutMs,
                readTimeoutMs, fallbackIds);
        return new EngagementReportClient(properties, "test-internal-key", RestClient.builder());
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
