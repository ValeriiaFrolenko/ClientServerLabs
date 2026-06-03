package core;

import model.Message;
import model.Packet;
import model.Product;
import network.Sender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import protocol.DecryptorService;
import protocol.EncryptorService;
import protocol.PacketDecoder;
import protocol.PacketEncoder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SystemConcurrencyTest {

    private static final byte[] KEY = "1234567890123456".getBytes();

    private ExecutorService decryptorPool;
    private ExecutorService processorPool;
    private ExecutorService encryptorPool;
    private ExecutorService senderPool;

    @BeforeEach
    public void setUp() {
        decryptorPool = Executors.newFixedThreadPool(2);
        processorPool = Executors.newFixedThreadPool(4);
        encryptorPool = Executors.newFixedThreadPool(3);
        senderPool = Executors.newFixedThreadPool(5);
    }

    @AfterEach
    public void tearDown() {
        decryptorPool.shutdownNow();
        processorPool.shutdownNow();
        encryptorPool.shutdownNow();
        senderPool.shutdownNow();
    }

    private Consumer<byte[]> buildPipeline(WareHouse wareHouse, CountDownLatch latch) {
        PacketEncoder encoder = new PacketEncoder(KEY);
        PacketDecoder decoder = new PacketDecoder(KEY);

        Sender sender = data -> latch.countDown();
        Consumer<byte[]> senderConsumer = data -> senderPool.submit(() -> sender.sendMessage(data));

        EncryptorService encryptorService = new EncryptorService(encoder, senderConsumer);
        Consumer<Packet> encryptorConsumer = packet -> encryptorPool.submit(() -> encryptorService.encrypt(packet));

        ProcessorService processor = new ProcessorService(wareHouse, encryptorConsumer);
        Consumer<Packet> processorConsumer = packet -> processorPool.submit(() -> processor.process(packet));

        DecryptorService decryptorService = new DecryptorService(decoder, processorConsumer);
        return data -> decryptorPool.submit(() -> decryptorService.decrypt(data));
    }

    @Test
    public void testConcurrentClientMessages() throws InterruptedException {
        int numberOfMessages = 100;
        CountDownLatch latch = new CountDownLatch(numberOfMessages);

        WareHouse wareHouse = new WareHouse();
        wareHouse.addProduct(new Product("Buckwheat", 50.0, 100));

        Consumer<byte[]> decryptorConsumer = buildPipeline(wareHouse, latch);
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
        assertEquals(1100, wareHouse.getProductQuantity("Buckwheat"));
    }

    @Test
    public void testMultipleClientsWithDifferentCommands() throws InterruptedException {
        int clients = 5;
        int messagesPerClient = 20;
        CountDownLatch latch = new CountDownLatch(clients * messagesPerClient);

        WareHouse wareHouse = new WareHouse();
        wareHouse.addProduct(new Product("Apple", 10.0, 1000));

        Consumer<byte[]> decryptorConsumer = buildPipeline(wareHouse, latch);
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
        assertEquals(1000, wareHouse.getProductQuantity("Apple"));
    }

    @Test
    public void testConcurrentIncreaseAndDecrease() throws InterruptedException {
        int operations = 50;
        CountDownLatch latch = new CountDownLatch(operations * 2);

        WareHouse wareHouse = new WareHouse();
        wareHouse.addProduct(new Product("Apple", 10.0, 100));

        Consumer<byte[]> decryptorConsumer = buildPipeline(wareHouse, latch);
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
        assertEquals(100, wareHouse.getProductQuantity("Apple"));
    }

    @Test
    public void testConcurrentSetPrice() throws InterruptedException {
        int operations = 30;
        CountDownLatch latch = new CountDownLatch(operations);

        WareHouse wareHouse = new WareHouse();
        wareHouse.addProduct(new Product("Apple", 10.0, 100));

        Consumer<byte[]> decryptorConsumer = buildPipeline(wareHouse, latch);
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

        double price = wareHouse.getProductPrice("Apple");
        assertTrue(price >= 1.0 && price <= 30.0);
    }

    @Test
    public void testHighLoad() throws InterruptedException {
        int numberOfMessages = 500;
        CountDownLatch latch = new CountDownLatch(numberOfMessages);

        WareHouse wareHouse = new WareHouse();
        wareHouse.addProduct(new Product("Apple", 10.0, 0));

        Consumer<byte[]> decryptorConsumer = buildPipeline(wareHouse, latch);
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
        assertEquals(500, wareHouse.getProductQuantity("Apple"));
    }
}