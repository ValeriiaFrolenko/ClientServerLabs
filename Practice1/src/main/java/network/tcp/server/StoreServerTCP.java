package network.tcp.server;

import core.ProcessorService;
import core.WareHouse;
import model.Packet;
import model.Product;
import network.Sender;
import network.tcp.TcpReceiver;
import network.tcp.TcpSender;
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
    private final ConnectionManager connectionManager = new ConnectionManager();
    private final Consumer<byte[]> decryptorConsumer;

    public StoreServerTCP() {
        byte[] key = "1234567890123456".getBytes();

        WareHouse wareHouse = new WareHouse();
        wareHouse.addProduct(new Product("Apple", 10.0, 100));

        ExecutorService senderPool = Executors.newFixedThreadPool(5);
        ExecutorService encryptorPool = Executors.newFixedThreadPool(3);
        ExecutorService processorPool = Executors.newFixedThreadPool(4);
        ExecutorService decryptorPool = Executors.newFixedThreadPool(2);

        Sender sender = new TcpSender(connectionManager);
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

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                clientPool.submit(() -> handleClient(clientSocket));
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        TcpReceiver receiver = new TcpReceiver(clientSocket, decryptorConsumer, connectionManager);
        receiver.run();
    }
}