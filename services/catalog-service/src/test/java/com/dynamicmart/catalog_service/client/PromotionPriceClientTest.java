package com.dynamicmart.catalog_service.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.dynamicmart.catalog_service.config.CatalogPromotionProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

class PromotionPriceClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void readsPromotionPriceContract() throws Exception {
        UUID variantId = UUID.randomUUID();
        UUID promotionId = UUID.randomUUID();
        startServer(exchange -> respond(exchange, HttpStatus.OK.value(), """
                [{
                  "variantId":"%s",
                  "listPriceVnd":100000,
                  "discountVnd":20000,
                  "salePriceVnd":80000,
                  "discountPercent":20,
                  "promotionId":"%s",
                  "promotionName":"Flash sale",
                  "endsAt":"2027-01-01T00:00:00Z"
                }]
                """.formatted(variantId, promotionId)));
        PromotionPriceClient client = client(500, 500);

        List<PromotionPriceClient.PriceResponse> result = client.resolve(
                List.of(new PromotionPriceClient.PriceRequest(variantId, 100_000)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).variantId()).isEqualTo(variantId);
        assertThat(result.get(0).salePriceVnd()).isEqualTo(80_000);
    }

    @Test
    void returnsListPriceFallbackInputWhenCartRespondsTooSlowly() throws Exception {
        UUID variantId = UUID.randomUUID();
        startServer(exchange -> {
            try {
                Thread.sleep(350);
                respond(exchange, HttpStatus.OK.value(), "[]");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (IOException ignored) {
                // The client is expected to close the connection after its read timeout.
            }
        });
        PromotionPriceClient client = client(100, 75);

        long startedAt = System.nanoTime();
        List<PromotionPriceClient.PriceResponse> result = client.resolve(
                List.of(new PromotionPriceClient.PriceRequest(variantId, 100_000)));
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

        assertThat(result).isEmpty();
        assertThat(elapsed).isLessThan(Duration.ofSeconds(2));
    }

    private PromotionPriceClient client(int connectTimeoutMs, int readTimeoutMs) {
        CatalogPromotionProperties properties = new CatalogPromotionProperties(
                CatalogPromotionProperties.Mode.HTTP, baseUrl(), connectTimeoutMs,
                readTimeoutMs, 15, 120);
        return new PromotionPriceClient(properties, RestClient.builder());
    }

    private void startServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/cart/promotions/prices/resolve", handler);
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
