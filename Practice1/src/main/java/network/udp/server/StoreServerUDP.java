package network.udp.server;

import core.ProcessorService;
import core.ProductService;
import database.DatabaseConnection;
import database.JdbcTemplate;
import database.ProductRepository;
import model.Packet;
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

        Consumer<Packet> encryptorConsumer = getPacketConsumer(key);

        ProcessorService processor = new ProcessorService(getService(), encryptorConsumer);
        Consumer<Packet> processorConsumer = packet -> processorPool.submit(() -> processor.process(packet));

        PacketDecoder decoder = new PacketDecoder(key);
        DecryptorService decryptorService = new DecryptorService(decoder, processorConsumer);
        this.decryptorConsumer = data -> decryptorPool.submit(() -> decryptorService.decrypt(data));
    }

    private Consumer<Packet> getPacketConsumer(byte[] key) {
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
        return encryptorConsumer;
    }

    private ProductService getService() {
        DatabaseConnection.init();
        JdbcTemplate jdbc = new JdbcTemplate(DatabaseConnection::getConnection);
        ProductRepository repository = new ProductRepository(jdbc);
        return new ProductService(repository);
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