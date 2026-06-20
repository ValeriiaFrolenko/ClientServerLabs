package http_network.server;

import com.sun.net.httpserver.HttpServer;
import database.DatabaseConnection;
import database.JdbcTemplate;
import database.ProductRepository;
import database.UserRepository;
import http_core.JwtService;
import http_handler.AuthHandler;
import http_handler.JwtAuthenticator;
import http_handler.ProductHandler;
import model.Product;
import service.ProductService;
import service.UserService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StoreServerHTTP {

    private static final int DEFAULT_PORT = 8082;
    private static final String JWT_SECRET = "warehouse-http-secret-key";

    private final int port;
    private final JdbcTemplate jdbc;
    private final ExecutorService handlerPool = Executors.newFixedThreadPool(8);
    private HttpServer server;

    public StoreServerHTTP() {
        this.port = DEFAULT_PORT;
        DatabaseConnection.init();
        this.jdbc = new JdbcTemplate(DatabaseConnection::getConnection);
    }

    public StoreServerHTTP(int port, JdbcTemplate jdbc) {
        this.port = port;
        this.jdbc = jdbc;
    }

    public void start() {
        start(null);
    }

    public void start(CountDownLatch ready) {
        ProductRepository productRepository = new ProductRepository(jdbc);
        ProductService productService = new ProductService(productRepository);
        seedDefaultProduct(productService);

        UserRepository userRepository = new UserRepository(jdbc);
        UserService userService = new UserService(userRepository);

        JwtService jwtService = new JwtService(JWT_SECRET);

        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start HTTP server on port " + port, e);
        }

        server.createContext("/login", new AuthHandler(userService, jwtService));
        server.createContext("/register", new AuthHandler(userService, jwtService));

        com.sun.net.httpserver.HttpContext productsContext =
                server.createContext("/products", new ProductHandler(productService));
        productsContext.setAuthenticator(new JwtAuthenticator(jwtService));

        server.setExecutor(handlerPool);
        server.start();
        System.out.println("HTTP server started on port " + port);

        if (ready != null) {
            ready.countDown();
        }
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