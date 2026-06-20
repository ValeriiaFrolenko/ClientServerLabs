package tcp_udp_core;

import database.JdbcTemplate;
import database.ProductRepository;
import model.Message;
import model.Packet;
import model.Product;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.ProductService;
import tcp_udp_protocol.DecryptorService;
import tcp_udp_protocol.EncryptorService;
import tcp_udp_protocol.PacketDecoder;
import tcp_udp_protocol.PacketEncoder;

import java.sql.DriverManager;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SystemConcurrencyTest {

    private static final byte[] KEY = "1234567890123456".getBytes();
    private static final String URL = "jdbc:h2:mem:concurrencydb;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private ExecutorService decryptorPool;
    private ExecutorService processorPool;
    private ExecutorService encryptorPool;
    private ExecutorService senderPool;

    private ProductService productService;

    @BeforeEach
    public void setUp() throws Exception {
        try (var conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS products");
            stmt.execute("""
                    CREATE TABLE products (
                        id       BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name     VARCHAR(255) NOT NULL,
                        category VARCHAR(255) NOT NULL,
                        price    DOUBLE       NOT NULL,
                        quantity INT          NOT NULL
                    )
                    """);
        }

        // Фабрика — кожен потік отримує своє окреме з'єднання
        JdbcTemplate jdbc = new JdbcTemplate(
                () -> DriverManager.getConnection(URL, USER, PASSWORD)
        );
        ProductRepository repository = new ProductRepository(jdbc);
        productService = new ProductService(repository);

        decryptorPool = Executors.newFixedThreadPool(2);
        processorPool = Executors.newFixedThreadPool(4);
        encryptorPool = Executors.newFixedThreadPool(3);
        senderPool = Executors.newFixedThreadPool(5);
    }

    @AfterEach
    public void tearDown() throws Exception {
        decryptorPool.shutdownNow();
        processorPool.shutdownNow();
        encryptorPool.shutdownNow();
        senderPool.shutdownNow();

        try (var conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS products");
        }
    }

    private Consumer<byte[]> buildPipeline(CountDownLatch latch) {
        PacketEncoder encoder = new PacketEncoder(KEY);
        PacketDecoder decoder = new PacketDecoder(KEY);

        Consumer<byte[]> senderConsumer = _ -> senderPool.submit(latch::countDown);

        EncryptorService encryptorService = new EncryptorService(encoder, senderConsumer);
        Consumer<Packet> encryptorConsumer = packet -> encryptorPool.submit(() -> encryptorService.encrypt(packet));

        ProcessorService processor = new ProcessorService(productService, encryptorConsumer);
        Consumer<Packet> processorConsumer = packet -> processorPool.submit(() -> processor.process(packet));

        DecryptorService decryptorService = new DecryptorService(decoder, processorConsumer);
        return data -> decryptorPool.submit(() -> decryptorService.decrypt(data));
    }

    @Test
    public void testConcurrentClientMessages() throws InterruptedException {
        int numberOfMessages = 100;
        CountDownLatch latch = new CountDownLatch(numberOfMessages);

        productService.create(Product.builder().name("Buckwheat").category("Grains").price(50.0).quantity(100).build());

        Consumer<byte[]> decryptorConsumer = buildPipeline(latch);
        PacketEncoder encoder = new PacketEncoder(KEY);

        ExecutorService clientPool = Executors.newFixedThreadPool(10);
        for (int i = 0; i < numberOfMessages; i++) {
            clientPool.submit(() -> {
                try {
                    Message message = new Message(Command.INCREASE_QUANTITY.ordinal(), 1, "Buckwheat:10");
                    Packet packet = new Packet((byte) 1, System.currentTimeMillis(), message);
                    decryptorConsumer.accept(encoder.encode(packet));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        clientPool.shutdownNow();
        assertEquals(1100, productService.getQuantity("Buckwheat"));
    }

    @Test
    public void testMultipleClientsWithDifferentCommands() throws InterruptedException {
        int clients = 5;
        int messagesPerClient = 20;
        CountDownLatch latch = new CountDownLatch(clients * messagesPerClient);

        productService.create(Product.builder().name("Apple").category("Fruit").price(10.0).quantity(1000).build());

        Consumer<byte[]> decryptorConsumer = buildPipeline(latch);
        PacketEncoder encoder = new PacketEncoder(KEY);

        ExecutorService clientPool = Executors.newFixedThreadPool(clients);
        for (byte clientId = 1; clientId <= clients; clientId++) {
            final byte id = clientId;
            clientPool.submit(() -> {
                for (int i = 0; i < messagesPerClient; i++) {
                    try {
                        Message message = new Message(Command.GET_QUANTITY.ordinal(), id, "Apple");
                        Packet packet = new Packet(id, System.currentTimeMillis(), message);
                        decryptorConsumer.accept(encoder.encode(packet));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        clientPool.shutdownNow();
        assertEquals(1000, productService.getQuantity("Apple"));
    }

    @Test
    public void testConcurrentIncreaseAndDecrease() throws InterruptedException {
        int operations = 50;
        CountDownLatch latch = new CountDownLatch(operations * 2);

        productService.create(Product.builder().name("Apple").category("Fruit").price(10.0).quantity(100).build());

        Consumer<byte[]> decryptorConsumer = buildPipeline(latch);
        PacketEncoder encoder = new PacketEncoder(KEY);

        ExecutorService pool = Executors.newFixedThreadPool(10);

        for (int i = 0; i < operations; i++) {
            pool.submit(() -> {
                try {
                    Message message = new Message(Command.INCREASE_QUANTITY.ordinal(), 1, "Apple:5");
                    Packet packet = new Packet((byte) 1, System.currentTimeMillis(), message);
                    decryptorConsumer.accept(encoder.encode(packet));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        for (int i = 0; i < operations; i++) {
            pool.submit(() -> {
                try {
                    Message message = new Message(Command.DECREASE_QUANTITY.ordinal(), 1, "Apple:5");
                    Packet packet = new Packet((byte) 1, System.currentTimeMillis(), message);
                    decryptorConsumer.accept(encoder.encode(packet));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        pool.shutdownNow();
        assertEquals(100, productService.getQuantity("Apple"));
    }

    @Test
    public void testConcurrentSetPrice() throws InterruptedException {
        int operations = 30;
        CountDownLatch latch = new CountDownLatch(operations);

        productService.create(Product.builder().name("Apple").category("Fruit").price(10.0).quantity(100).build());

        Consumer<byte[]> decryptorConsumer = buildPipeline(latch);
        PacketEncoder encoder = new PacketEncoder(KEY);

        ExecutorService pool = Executors.newFixedThreadPool(10);
        for (int i = 1; i <= operations; i++) {
            final int price = i;
            pool.submit(() -> {
                try {
                    Message message = new Message(Command.SET_PRICE.ordinal(), 1, "Apple:" + price + ".0");
                    Packet packet = new Packet((byte) 1, System.currentTimeMillis(), message);
                    decryptorConsumer.accept(encoder.encode(packet));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        pool.shutdownNow();

        double price = productService.getByName("Apple").price();
        assertTrue(price >= 1.0 && price <= 30.0);
    }

    @Test
    public void testHighLoad() throws InterruptedException {
        int numberOfMessages = 500;
        CountDownLatch latch = new CountDownLatch(numberOfMessages);

        productService.create(Product.builder().name("Apple").category("Fruit").price(10.0).quantity(0).build());

        Consumer<byte[]> decryptorConsumer = buildPipeline(latch);
        PacketEncoder encoder = new PacketEncoder(KEY);

        ExecutorService pool = Executors.newFixedThreadPool(20);
        for (int i = 0; i < numberOfMessages; i++) {
            pool.submit(() -> {
                try {
                    Message message = new Message(Command.INCREASE_QUANTITY.ordinal(), 1, "Apple:1");
                    Packet packet = new Packet((byte) 1, System.currentTimeMillis(), message);
                    decryptorConsumer.accept(encoder.encode(packet));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        assertTrue(latch.await(15, TimeUnit.SECONDS));
        pool.shutdownNow();
        assertEquals(500, productService.getQuantity("Apple"));
    }
}