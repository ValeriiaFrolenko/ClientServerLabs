package http_network.server;

import database.JdbcTemplate;
import http_network.server.StoreServerHTTP;
import model.Product;
import org.junit.jupiter.api.*;
import utils.JsonUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

class StoreServerHTTPTest {

    private static final String BASE_URL = "http://127.0.0.1:8082";
    private static final String DB_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private static StoreServerHTTP server;
    private static HttpClient http;
    private static String token;

    @BeforeAll
    static void startServer() throws Exception {
        try (var conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS products (
                        id       BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name     VARCHAR(255) NOT NULL,
                        category VARCHAR(255) NOT NULL,
                        price    DOUBLE       NOT NULL,
                        quantity INT          NOT NULL
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                        login         VARCHAR(255) NOT NULL UNIQUE,
                        password_hash VARCHAR(255) NOT NULL
                    )
                    """);
        }

        JdbcTemplate jdbc = new JdbcTemplate(
                () -> DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)
        );

        CountDownLatch ready = new CountDownLatch(1);
        server = new StoreServerHTTP(8082, jdbc);
        Thread serverThread = new Thread(() -> server.start(ready));
        serverThread.setDaemon(true);
        serverThread.start();
        ready.await();

        http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        // register + login to get token
        byte[] regBody = JsonUtil.toBytes(Map.of("login", "testuser", "password", "testpass"));
        http.send(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(regBody))
                .build(), HttpResponse.BodyHandlers.ofByteArray());

        byte[] loginBody = JsonUtil.toBytes(Map.of("login", "testuser", "password", "testpass"));
        HttpResponse<byte[]> loginResp = http.send(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(loginBody))
                .build(), HttpResponse.BodyHandlers.ofByteArray());

        Map<String, Object> loginJson = JsonUtil.parseMap(loginResp.body());
        token = (String) loginJson.get("token");
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    void clearProducts() throws Exception {
        try (var conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM products");
        }
    }

    // --- login ---

    @Test
    void login_validCredentials_returns200AndToken() throws Exception {
        byte[] body = JsonUtil.toBytes(Map.of("login", "testuser", "password", "testpass"));
        HttpResponse<byte[]> response = http.send(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build(), HttpResponse.BodyHandlers.ofByteArray());

        assertEquals(200, response.statusCode());
        Map<String, Object> json = JsonUtil.parseMap(response.body());
        assertNotNull(json.get("token"));
    }

    @Test
    void login_invalidPassword_returns401() throws Exception {
        byte[] body = JsonUtil.toBytes(Map.of("login", "testuser", "password", "wrongpass"));
        HttpResponse<byte[]> response = http.send(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build(), HttpResponse.BodyHandlers.ofByteArray());

        assertEquals(401, response.statusCode());
    }

    // --- auth ---

    @Test
    void getProduct_withoutToken_returns401() throws Exception {
        HttpResponse<byte[]> response = http.send(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/products/1"))
                .GET()
                .build(), HttpResponse.BodyHandlers.ofByteArray());

        assertEquals(401, response.statusCode());
    }

    // --- PUT /products ---

    @Test
    void createProduct_returns201() throws Exception {
        HttpResponse<byte[]> response = putProduct("Banana", "Fruits", 1.5, 100);

        assertEquals(201, response.statusCode());
        Map<String, Object> json = JsonUtil.parseMap(response.body());
        assertEquals("Banana", json.get("name"));
        assertNotNull(json.get("id"));
    }

    @Test
    void createProduct_duplicateName_returns409() throws Exception {
        putProduct("Orange", "Fruits", 2.0, 50);
        HttpResponse<byte[]> response = putProduct("Orange", "Fruits", 2.0, 50);

        assertEquals(409, response.statusCode());
    }

    // --- GET /products/{id} ---

    @Test
    void getProduct_existingId_returns200() throws Exception {
        long id = extractId(putProduct("Mango", "Fruits", 3.0, 20));

        HttpResponse<byte[]> response = getProduct(id);

        assertEquals(200, response.statusCode());
        Map<String, Object> json = JsonUtil.parseMap(response.body());
        assertEquals("Mango", json.get("name"));
    }

    @Test
    void getProduct_nonExistingId_returns404() throws Exception {
        HttpResponse<byte[]> response = getProduct(99999L);

        assertEquals(404, response.statusCode());
    }

    // --- POST /products/{id} ---

    @Test
    void updateProduct_returns200WithUpdatedData() throws Exception {
        long id = extractId(putProduct("Apple", "Fruits", 1.0, 10));

        byte[] body = JsonUtil.toBytes(Map.of(
                "name", "Apple",
                "category", "UpdatedFruits",
                "price", 2.0,
                "quantity", 5
        ));
        HttpResponse<byte[]> response = http.send(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/products/" + id))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build(), HttpResponse.BodyHandlers.ofByteArray());

        assertEquals(200, response.statusCode());
        Map<String, Object> json = JsonUtil.parseMap(response.body());
        assertEquals("UpdatedFruits", json.get("category"));
        assertEquals(2.0, ((Number) json.get("price")).doubleValue());
    }

    // --- DELETE /products/{id} ---

    @Test
    void deleteProduct_returns204() throws Exception {
        long id = extractId(putProduct("Grape", "Fruits", 4.0, 30));

        HttpResponse<byte[]> response = http.send(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/products/" + id))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build(), HttpResponse.BodyHandlers.ofByteArray());

        assertEquals(204, response.statusCode());
    }

    @Test
    void deleteProduct_thenGet_returns404() throws Exception {
        long id = extractId(putProduct("Pear", "Fruits", 2.5, 15));

        http.send(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/products/" + id))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build(), HttpResponse.BodyHandlers.ofByteArray());

        HttpResponse<byte[]> response = getProduct(id);
        assertEquals(404, response.statusCode());
    }

    // --- helpers ---

    private HttpResponse<byte[]> putProduct(String name, String category, double price, int quantity) throws Exception {
        byte[] body = JsonUtil.toBytes(Map.of(
                "name", name,
                "category", category,
                "price", price,
                "quantity", quantity
        ));
        return http.send(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/products"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body))
                .build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private HttpResponse<byte[]> getProduct(long id) throws Exception {
        return http.send(HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/products/" + id))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private long extractId(HttpResponse<byte[]> response) {
        Map<String, Object> json = JsonUtil.parseMap(response.body());
        return ((Number) json.get("id")).longValue();
    }
}