package tcp_udp_network.tcp.client;

import tcp_udp_network.PacketGenerator;
import tcp_udp_network.ResponseHandler;
import tcp_udp_network.Sender;
import tcp_udp_network.tcp.TcpReceiver;
import tcp_udp_protocol.DecryptorService;
import tcp_udp_protocol.EncryptorService;
import tcp_udp_protocol.PacketDecoder;
import tcp_udp_protocol.PacketEncoder;
import model.Packet;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class StoreClientTCP {

    private static final String HOST = "localhost";
    private static final int PORT = 8080;
    private static final int RECONNECT_DELAY_MS = 3000;

    private final byte clientId;
    private final byte[] key = "1234567890123456".getBytes();

    private final ExecutorService encryptorPool = Executors.newFixedThreadPool(2);
    private final ExecutorService senderPool = Executors.newFixedThreadPool(2);
    private final ExecutorService decryptorPool = Executors.newFixedThreadPool(2);
    private final ExecutorService responsePool = Executors.newFixedThreadPool(2);

    public StoreClientTCP(byte clientId) {
        this.clientId = clientId;
    }

    public void start() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try (Socket socket = new Socket(HOST, PORT)) {
                    System.out.println("Connected to server");
                    run(socket);
                } catch (IOException e) {
                    System.err.println("Server unavailable, retrying in " + RECONNECT_DELAY_MS + "ms");
                    try {
                        Thread.sleep(RECONNECT_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } finally {
            stop();
        }
    }

    private void run(Socket socket) {
        PacketDecoder decoder = new PacketDecoder(key);
        PacketEncoder encoder = new PacketEncoder(key);

        Sender sender = new TcpClientSender(socket);
        Consumer<byte[]> senderConsumer = data -> senderPool.submit(() -> sender.sendMessage(data));

        EncryptorService encryptorService = new EncryptorService(encoder, senderConsumer);
        Consumer<Packet> encryptorConsumer = packet -> encryptorPool.submit(() -> encryptorService.encrypt(packet));

        ResponseHandler responseHandler = new ResponseHandler(packet ->
                responsePool.submit(() ->
                        System.out.println("Response: " + packet.getbMsq().getMessage())));
        DecryptorService decryptorService = new DecryptorService(decoder, responseHandler::handle);
        Consumer<byte[]> decryptorConsumer = data -> decryptorPool.submit(() -> decryptorService.decrypt(data));

        PacketGenerator generator = new PacketGenerator(clientId);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            pool.submit(() -> sendLoop(socket, generator, encryptorConsumer));
            pool.submit(() -> new TcpReceiver(socket, decryptorConsumer).run());
        }
    }

    private void stop() {
        encryptorPool.shutdown();
        senderPool.shutdown();
        decryptorPool.shutdown();
        responsePool.shutdown();
    }

    private void sendLoop(Socket socket, PacketGenerator generator, Consumer<Packet> encryptorConsumer) {
        while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
            try {
                encryptorConsumer.accept(generator.generate());
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Send failed: " + e.getMessage());
                break;
            }
        }
    }
}