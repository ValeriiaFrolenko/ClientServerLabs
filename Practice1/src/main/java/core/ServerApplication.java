package core;

import model.Product;
import network.FakeReceiver;
import network.FakeSender;
import network.Sender;
import protocol.DecryptorImp;
import protocol.EncryptorImp;
import protocol.PacketDecryptor;
import protocol.PacketEncryptor;
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

        PacketEncryptor packetEncryptor = new PacketEncryptor(key);
        EncryptorImp encryptorImp = new EncryptorImp(packetEncryptor, senderConsumer);
        Consumer<Packet> encryptorConsumer = packet -> encryptorPool.submit(() -> encryptorImp.encryptMessage(packet));

        ProcessorImp processor = new ProcessorImp(wareHouse, encryptorConsumer);
        Consumer<Packet> processorConsumer = packet -> processorPool.submit(() -> processor.process(packet));

        PacketDecryptor packetDecryptor = new PacketDecryptor(key);
        DecryptorImp decryptor = new DecryptorImp(packetDecryptor, processorConsumer);
        Consumer<byte[]> decryptorConsumer = data -> decryptorPool.submit(() -> decryptor.decrypt(data));

        FakeReceiver receiver1 = new FakeReceiver(decryptorConsumer);
        FakeReceiver receiver2 = new FakeReceiver(decryptorConsumer);

        receiverPool.submit(receiver1);
        receiverPool.submit(receiver2);
    }

    public void stop() {
        receiverPool.shutdownNow();
        decryptorPool.shutdownNow();
        processorPool.shutdownNow();
        encryptorPool.shutdownNow();
        senderPool.shutdownNow();
        try {
            receiverPool.awaitTermination(2, TimeUnit.SECONDS);
            decryptorPool.awaitTermination(2, TimeUnit.SECONDS);
            processorPool.awaitTermination(2, TimeUnit.SECONDS);
            encryptorPool.awaitTermination(2, TimeUnit.SECONDS);
            senderPool.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}