package http_core;

import com.sun.net.httpserver.HttpExchange;
import utils.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpResponses {

    private HttpResponses() {}

    public static void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] bytes = JsonUtil.toBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        sendJson(exchange, statusCode, body);
    }

    public static void sendEmpty(HttpExchange exchange, int statusCode) throws IOException {
        exchange.sendResponseHeaders(statusCode, -1);
        exchange.close();
    }

    public static Map<String, Object> readJsonBody(HttpExchange exchange) throws IOException {
        byte[] data = exchange.getRequestBody().readAllBytes();
        return JsonUtil.parseMap(data);
    }
}