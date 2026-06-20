package network.http.server;

import com.sun.net.httpserver.HttpServer;
import core.ProductService;
import core.UserService;
import database.DatabaseConnection;
import database.JdbcTemplate;
import database.ProductRepository;
import database.UserRepository;
import http_core.JwtService;
import http_handler.AuthHandler;
import http_handler.Authenticator;
import http_handler.ProductHandler;
import model.Product;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StoreServerHTTP {

    private static final int PORT = 8082;
    private static final String JWT_SECRET = "warehouse-http-secret-key";

    private final ExecutorService handlerPool = Executors.newFixedThreadPool(8);
    private HttpServer server;

    public void start() {
        DatabaseConnection.init();
        JdbcTemplate jdbc = new JdbcTemplate(DatabaseConnection::getConnection);

        ProductRepository productRepository = new ProductRepository(jdbc);
        ProductService productService = new ProductService(productRepository);
        seedDefaultProduct(productService);

        UserRepository userRepository = new UserRepository(jdbc);
        UserService userService = new UserService(userRepository);

        JwtService jwtService = new JwtService(JWT_SECRET);

        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start HTTP server on port " + PORT, e);
        }

        server.createContext("/login", new AuthHandler(userService, jwtService));
        server.createContext("/register", new AuthHandler(userService, jwtService));
        server.createContext("/products", new Authenticator(jwtService, new ProductHandler(productService)));

        server.setExecutor(handlerPool);
        server.start();
        System.out.println("HTTP server started on port " + PORT);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
        handlerPool.shutdown();
    }

    private void seedDefaultProduct(ProductService service) {
        try {
            service.getByName("Apple");
        } catch (NoSuchElementException e) {
            service.create(Product.builder().id(0).name("Apple").category("Fruits").price(100).quantity(100).build());
        }
    }
}