package network.udp.server;

import core.ProcessorService;
import core.WareHouse;
import model.Packet;
import model.Product;
import network.Sender;
import network.udp.UdpReceiver;
import protocol.DecryptorService;
import protocol.EncryptorService;
import protocol.PacketDecoder;
import protocol.PacketEncoder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class StoreServerUDP {

    private static final int PORT = 8081;
    private static final int BUFFER_SIZE = 4096;

    private DatagramSocket socket;
    private final UdpConnectionManager connectionManager = new UdpConnectionManager();
    private final Consumer<byte[]> decryptorConsumer;
    private final ExecutorService receiverPool = Executors.newFixedThreadPool(5);
    private final ExecutorService senderPool = Executors.newFixedThreadPool(3);
    private final ExecutorService encryptorPool = Executors.newFixedThreadPool(3);
    private final ExecutorService processorPool = Executors.newFixedThreadPool(4);
    private final ExecutorService decryptorPool = Executors.newFixedThreadPool(2);

    public StoreServerUDP() {
        byte[] key = "1234567890123456".getBytes();

        WareHouse wareHouse = new WareHouse();
        wareHouse.addProduct(new Product("Apple", 10.0, 100));

        Sender sender = new UdpServerSender(connectionManager, datagram -> senderPool.submit(() -> {
            try {
                socket.send(datagram);
            } catch (IOException e) {
                System.err.println("UdpSender error: " + e.getMessage());
            }
        }));

        Consumer<byte[]> senderConsumer = data -> senderPool.submit(() -> sender.sendMessage(data));

        PacketEncoder encoder = new PacketEncoder(key);
        EncryptorService encryptorService = new EncryptorService(encoder, senderConsumer);
        Consumer<Packet> encryptorConsumer = packet -> encryptorPool.submit(() -> encryptorService.encrypt(packet));

        ProcessorService processor = new ProcessorService(wareHouse, encryptorConsumer);
        Consumer<Packet> processorConsumer = packet -> processorPool.submit(() -> processor.process(packet));

        PacketDecoder decoder = new PacketDecoder(key);
        DecryptorService decryptorService = new DecryptorService(decoder, processorConsumer);
        this.decryptorConsumer = data -> decryptorPool.submit(() -> decryptorService.decrypt(data));
    }

    public void start() {
        try (DatagramSocket ds = new DatagramSocket(PORT); connectionManager) {
            this.socket = ds;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
                    socket.receive(datagram);
                    receiverPool.submit(() -> new UdpReceiver(datagram, connectionManager, decryptorConsumer).receive());
                } catch (IOException e) {
                    System.err.println("UdpServer receive error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("UdpServer error: " + e.getMessage());
        } finally {
            stop();
        }
    }

    private void stop() {
        receiverPool.shutdown();
        senderPool.shutdown();
        encryptorPool.shutdown();
        processorPool.shutdown();
        decryptorPool.shutdown();
    }
}