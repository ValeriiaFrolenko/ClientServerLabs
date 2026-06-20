package http_handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import core.ProductService;
import database.JdbcTemplate;
import model.Product;
import http_core.HttpResponses;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class ProductHandler implements HttpHandler {

    private final ProductService productService;

    public ProductHandler(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            switch (method) {
                case "GET" -> handleGet(exchange, path);
                case "PUT" -> handlePut(exchange, path);
                case "POST" -> handlePost(exchange, path);
                case "DELETE" -> handleDelete(exchange, path);
                default -> HttpResponses.sendError(exchange, 405, "Method not allowed");
            }
        } catch (IllegalArgumentException e) {
            HttpResponses.sendError(exchange, 400, e.getMessage());
        }
    }

    private void handleGet(HttpExchange exchange, String path) throws IOException {
        Long id = extractId(path);
        if (id == null) {
            HttpResponses.sendError(exchange, 400, "Product id is required: GET /products/{id}");
            return;
        }
        try {
            Product product = productService.getById(id);
            HttpResponses.sendJson(exchange, 200, toMap(product));
        } catch (NoSuchElementException e) {
            HttpResponses.sendError(exchange, 404, "Product not found: id=" + id);
        }
    }

    private void handlePut(HttpExchange exchange, String path) throws IOException {
        if (!path.equals("/products")) {
            HttpResponses.sendError(exchange, 404, "Not found");
            return;
        }

        Map<String, Object> body = HttpResponses.readJsonBody(exchange);
        Product input;
        try {
            input = fromMap(body, 0L);
        } catch (Exception e) {
            HttpResponses.sendError(exchange, 400, "Malformed product: " + e.getMessage());
            return;
        }

        try {
            productService.getByName(input.name());
            HttpResponses.sendError(exchange, 409, "Product with this name already exists: " + input.name());
        } catch (NoSuchElementException notFound) {
            Product created = productService.create(input);
            HttpResponses.sendJson(exchange, 201, toMap(created));
        }
    }

    private void handlePost(HttpExchange exchange, String path) throws IOException {
        Long id = extractId(path);
        if (id == null) {
            HttpResponses.sendError(exchange, 400, "Product id is required: POST /products/{id}");
            return;
        }

        Map<String, Object> body = HttpResponses.readJsonBody(exchange);
        Product input;
        try {
            input = fromMap(body, id);
        } catch (Exception e) {
            HttpResponses.sendError(exchange, 400, "Malformed product: " + e.getMessage());
            return;
        }

        try {
            Product updated = productService.update(input);
            HttpResponses.sendJson(exchange, 200, toMap(updated));
        } catch (JdbcTemplate.DatabaseException e) {
            HttpResponses.sendError(exchange, 404, "Product not found: id=" + id);
        }
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        Long id = extractId(path);
        if (id == null) {
            HttpResponses.sendError(exchange, 400, "Product id is required: DELETE /products/{id}");
            return;
        }
        try {
            productService.delete(id);
            HttpResponses.sendEmpty(exchange, 204);
        } catch (JdbcTemplate.DatabaseException e) {
            HttpResponses.sendError(exchange, 404, "Product not found: id=" + id);
        }
    }

    private Long extractId(String path) {
        String prefix = "/products/";
        if (!path.startsWith(prefix)) return null;
        String idPart = path.substring(prefix.length());
        if (idPart.isEmpty()) return null;
        try {
            return Long.parseLong(idPart);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid product id: " + idPart);
        }
    }

    private Product fromMap(Map<String, Object> body, long fallbackId) {
        long id = body.containsKey("id") ? toLong(body.get("id")) : fallbackId;
        String name = (String) body.get("name");
        String category = (String) body.get("category");
        double price = toDouble(body.get("price"));
        int quantity = (int) toLong(body.get("quantity"));

        if (name == null || category == null) {
            throw new IllegalArgumentException("Fields 'name' and 'category' are required");
        }

        return Product.builder()
                .id(id)
                .name(name)
                .category(category)
                .price(price)
                .quantity(quantity)
                .build();
    }

    private long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        throw new IllegalArgumentException("Expected a number, got: " + value);
    }

    private double toDouble(Object value) {
        if (value instanceof Number n) return n.doubleValue();
        throw new IllegalArgumentException("Expected a number, got: " + value);
    }

    private Map<String, Object> toMap(Product product) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", product.id());
        map.put("name", product.name());
        map.put("category", product.category());
        map.put("price", product.price());
        map.put("quantity", product.quantity());
        return map;
    }
}