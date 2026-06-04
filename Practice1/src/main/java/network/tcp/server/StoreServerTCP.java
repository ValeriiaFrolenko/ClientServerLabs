package network.tcp.server;

import core.ProcessorService;
import core.ProductService;
import database.DatabaseConnection;
import database.JdbcTemplate;
import database.ProductRepository;
import model.Packet;
import network.Sender;
import network.tcp.TcpReceiver;
import protocol.DecryptorService;
import protocol.EncryptorService;
import protocol.PacketDecoder;
import protocol.PacketEncoder;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class StoreServerTCP {

    private static final int PORT = 8080;

    private final ExecutorService clientPool = Executors.newCachedThreadPool();
    private final ExecutorService senderPool = Executors.newFixedThreadPool(5);
    private final ExecutorService encryptorPool = Executors.newFixedThreadPool(3);
    private final ExecutorService processorPool = Executors.newFixedThreadPool(4);
    private final ExecutorService decryptorPool = Executors.newFixedThreadPool(2);

    private final TcpConnectionManager tcpConnectionManager = new TcpConnectionManager();
    private final Consumer<byte[]> decryptorConsumer;

    public StoreServerTCP() {
        byte[] key = "1234567890123456".getBytes();

        Sender sender = new TcpServerSender(tcpConnectionManager);
        Consumer<byte[]> senderConsumer = data -> senderPool.submit(() -> sender.sendMessage(data));

        PacketEncoder encoder = new PacketEncoder(key);
        EncryptorService encryptorService = new EncryptorService(encoder, senderConsumer);
        Consumer<Packet> encryptorConsumer = packet -> encryptorPool.submit(() -> encryptorService.encrypt(packet));

        ProcessorService processor = new ProcessorService(getService(), encryptorConsumer);
        Consumer<Packet> processorConsumer = packet -> processorPool.submit(() -> processor.process(packet));

        PacketDecoder decoder = new PacketDecoder(key);
        DecryptorService decryptorService = new DecryptorService(decoder, processorConsumer);
        this.decryptorConsumer = data -> decryptorPool.submit(() -> decryptorService.decrypt(data));
    }

    private ProductService getService() {
        DatabaseConnection.init();
        JdbcTemplate jdbc = new JdbcTemplate(DatabaseConnection::getConnection);
        ProductRepository repository = new ProductRepository(jdbc);
        return new ProductService(repository);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                clientPool.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            stop();
        }
    }

    private void handleClient(Socket clientSocket) {
        TcpReceiver receiver = new TcpReceiver(clientSocket, decryptorConsumer, tcpConnectionManager);
        receiver.run();
    }

    private void stop() {
        clientPool.shutdown();
        senderPool.shutdown();
        encryptorPool.shutdown();
        processorPool.shutdown();
        decryptorPool.shutdown();
    }
}