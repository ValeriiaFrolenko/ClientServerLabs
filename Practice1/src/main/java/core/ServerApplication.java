package core;

import model.Product;
import network.FakeReceiver;
import network.FakeSender;
import network.Sender;
import protocol.DecryptorService;
import protocol.EncryptorService;
import protocol.PacketDecoder;
import protocol.PacketEncoder;
import model.Packet;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ServerApplication {

    private final ExecutorService receiverPool = Executors.newFixedThreadPool(2);
    private final ExecutorService decryptorPool = Executors.newFixedThreadPool(2);
    private final ExecutorService processorPool = Executors.newFixedThreadPool(4);
    private final ExecutorService encryptorPool = Executors.newFixedThreadPool(3);
    private final ExecutorService senderPool = Executors.newFixedThreadPool(5);

    public void start() {
        WareHouse wareHouse = new WareHouse();
        wareHouse.addProduct(new Product("Apple", 10.0, 100));

        byte[] key = "1234567890123456".getBytes();

        Sender sender = new FakeSender();
        Consumer<byte[]> senderConsumer = data -> senderPool.submit(() -> sender.sendMessage(data));

        PacketEncoder packetEncoder = new PacketEncoder(key);
        EncryptorService encryptorService = new EncryptorService(packetEncoder, senderConsumer);
        Consumer<Packet> encryptorConsumer = packet -> encryptorPool.submit(() -> encryptorService.encryptMessage(packet));

        ProcessorService processor = new ProcessorService(wareHouse, encryptorConsumer);
        Consumer<Packet> processorConsumer = packet -> processorPool.submit(() -> processor.process(packet));

        PacketDecoder packetDecoder = new PacketDecoder(key);
        DecryptorService decryptorService = new DecryptorService(packetDecoder, processorConsumer);
        Consumer<byte[]> decryptorConsumer = data -> decryptorPool.submit(() -> decryptorService.decrypt(data));

        FakeReceiver receiver1 = new FakeReceiver(decryptorConsumer);
        FakeReceiver receiver2 = new FakeReceiver(decryptorConsumer);

        receiverPool.submit(receiver1);
        receiverPool.submit(receiver2);
    }


    public boolean stop() {
        receiverPool.shutdownNow();
        decryptorPool.shutdownNow();
        processorPool.shutdownNow();
        encryptorPool.shutdownNow();
        senderPool.shutdownNow();
        try {
            boolean receiversStopped = receiverPool.awaitTermination(2, TimeUnit.SECONDS);
            boolean decryptorsStopped = decryptorPool.awaitTermination(2, TimeUnit.SECONDS);
            boolean processorsStopped = processorPool.awaitTermination(2, TimeUnit.SECONDS);
            boolean encryptorsStopped = encryptorPool.awaitTermination(2, TimeUnit.SECONDS);
            boolean sendersStopped = senderPool.awaitTermination(2, TimeUnit.SECONDS);

            return receiversStopped && decryptorsStopped && processorsStopped && encryptorsStopped && sendersStopped;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}