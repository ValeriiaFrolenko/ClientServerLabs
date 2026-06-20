package http_network.client;

import model.Product;
import utils.JsonUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class StoreClientHTTP implements AutoCloseable {

    private final String baseUrl;
    private final HttpClient http;
    private String token;

    public StoreClientHTTP(String baseUrl) {
        this.baseUrl = baseUrl;
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void login(String login, String password) throws Exception {
        byte[] body = JsonUtil.toBytes(Map.of("login", login, "password", password));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Login failed [" + response.statusCode() + "]: " + new String(response.body()));
        }

        Map<String, Object> responseBody = JsonUtil.parseMap(response.body());
        this.token = (String) responseBody.get("token");
        System.out.println("[HTTP] Logged in as " + login);
    }

    public void getProduct(long id) throws Exception {
        HttpRequest request = authorized(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/products/" + id))
                .GET())
                .build();

        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 404) {
            throw new RuntimeException("Product not found: id=" + id);
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException("getProduct failed [" + response.statusCode() + "]: " + new String(response.body()));
        }

        Product product = parseProduct(response.body());
        System.out.println("[HTTP] GET /products/" + id + " -> " + product);
    }

    public Product createProduct(Product product) throws Exception {
        byte[] body = JsonUtil.toBytes(Map.of(
                "name", product.name(),
                "category", product.category(),
                "price", product.price(),
                "quantity", product.quantity()
        ));

        HttpRequest request = authorized(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/products"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body)))
                .build();

        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 409) {
            throw new RuntimeException("Product already exists: " + product.name());
        }
        if (response.statusCode() != 201) {
            throw new RuntimeException("createProduct failed [" + response.statusCode() + "]: " + new String(response.body()));
        }

        Product created = parseProduct(response.body());
        System.out.println("[HTTP] PUT /products -> created " + created);
        return created;
    }

    public void updateProduct(Product product) throws Exception {
        byte[] body = JsonUtil.toBytes(Map.of(
                "name", product.name(),
                "category", product.category(),
                "price", product.price(),
                "quantity", product.quantity()
        ));

        HttpRequest request = authorized(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/products/" + product.id()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body)))
                .build();

        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 404) {
            throw new RuntimeException("Product not found: id=" + product.id());
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException("updateProduct failed [" + response.statusCode() + "]: " + new String(response.body()));
        }

        Product updated = parseProduct(response.body());
        System.out.println("[HTTP] POST /products/" + product.id() + " -> updated " + updated);
    }

    public void deleteProduct(long id) throws Exception {
        HttpRequest request = authorized(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/products/" + id))
                .DELETE())
                .build();

        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 404) {
            throw new RuntimeException("Product not found: id=" + id);
        }
        if (response.statusCode() != 204) {
            throw new RuntimeException("deleteProduct failed [" + response.statusCode() + "]: " + new String(response.body()));
        }

        System.out.println("[HTTP] DELETE /products/" + id + " -> deleted");
    }

    @Override
    public void close() {
        http.close();
    }

    private HttpRequest.Builder authorized(HttpRequest.Builder builder) {
        if (token == null) {
            throw new IllegalStateException("Not logged in. Call login() first.");
        }
        return builder.header("Authorization", "Bearer " + token);
    }

    private Product parseProduct(byte[] data) {
        Map<String, Object> map = JsonUtil.parseMap(data);
        return Product.builder()
                .id(((Number) map.get("id")).longValue())
                .name((String) map.get("name"))
                .category((String) map.get("category"))
                .price(((Number) map.get("price")).doubleValue())
                .quantity(((Number) map.get("quantity")).intValue())
                .build();
    }
}