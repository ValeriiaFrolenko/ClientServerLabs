import http_network.client.StoreClientHTTP;
import http_network.server.StoreServerHTTP;
import model.Product;
import utils.JsonUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class HttpApp {

    private static final String BASE_URL = "http://localhost:8082";
    private static final String TEST_LOGIN = "admin";
    private static final String TEST_PASSWORD = "admin123";

    public static void main(String[] args) throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        StoreServerHTTP server = new StoreServerHTTP();

        Thread serverThread = new Thread(() -> server.start(ready));
        serverThread.setUncaughtExceptionHandler((t, e) -> {
            System.err.println("[SERVER THREAD] Uncaught exception:" + e.getMessage());
        });
        serverThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[HTTP] Shutting down server...");
            server.stop();
        }));

        ready.await();

        try (StoreClientHTTP client = new StoreClientHTTP(BASE_URL)) {
            registerIfNeeded();
            client.login(TEST_LOGIN, TEST_PASSWORD);

            Product created = client.createProduct(Product.builder()
                    .name("TestProduct")
                    .category("TestCategory")
                    .price(99.99)
                    .quantity(50)
                    .build());

            client.getProduct(created.id());

            client.updateProduct(Product.builder()
                    .id(created.id())
                    .name(created.name())
                    .category("UpdatedCategory")
                    .price(149.99)
                    .quantity(25)
                    .build());

            client.getProduct(created.id());

            client.deleteProduct(created.id());

            try {
                client.getProduct(created.id());
            } catch (RuntimeException e) {
                System.out.println("[HTTP] Confirmed deleted: " + e.getMessage());
            }
        }
    }

    private static void registerIfNeeded() throws Exception {
        try (HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build()) {
            byte[] body = JsonUtil.toBytes(Map.of("login", TEST_LOGIN, "password", TEST_PASSWORD));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 201) {
                System.out.println("[HTTP] Registered user: " + TEST_LOGIN);
            } else if (response.statusCode() == 409) {
                System.out.println("[HTTP] User already exists: " + TEST_LOGIN);
            }
        }
    }
}