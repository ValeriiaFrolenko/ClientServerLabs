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
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SystemConcurrencyTest {

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

    @Test
    public void testConcurrentClientMessages() throws InterruptedException {
        int numberOfMessages = 100;
        CountDownLatch completionLatch = new CountDownLatch(numberOfMessages);

        WareHouse wareHouse = new WareHouse();
        wareHouse.addProduct(new Product("Buckwheat", 50.0, 100));

        byte[] key = "1234567890123456".getBytes();

        Sender sender = data -> completionLatch.countDown();
        Consumer<byte[]> senderConsumer = data -> senderPool.submit(() -> sender.sendMessage(data));

        PacketEncoder packetEncoder = new PacketEncoder(key);
        EncryptorService encryptorService = new EncryptorService(packetEncoder, senderConsumer);
        Consumer<Packet> encryptorConsumer = packet -> encryptorPool.submit(() -> encryptorService.encrypt(packet));

        ProcessorService processor = new ProcessorService(wareHouse, encryptorConsumer);
        Consumer<Packet> processorConsumer = packet -> processorPool.submit(() -> processor.process(packet));

        PacketDecoder packetDecoder = new PacketDecoder(key);
        DecryptorService decryptorService = new DecryptorService(packetDecoder, processorConsumer);
        Consumer<byte[]> decryptorConsumer = data -> decryptorPool.submit(() -> decryptorService.decrypt(data));

        ExecutorService clientPool = Executors.newFixedThreadPool(10);

        for (int i = 0; i < numberOfMessages; i++) {
            clientPool.submit(() -> {
                try {
                    Message message = new Message(Command.INCREASE_QUANTITY.ordinal(), 1, "Buckwheat:10");
                    Packet packet = new Packet((byte) 1, 1L, message);
                    byte[] encryptedPacket = packetEncoder.encode(packet);
                    decryptorConsumer.accept(encryptedPacket);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        completionLatch.await();
        clientPool.shutdownNow();

        assertEquals(1100, wareHouse.getProductQuantity("Buckwheat"));
    }
}