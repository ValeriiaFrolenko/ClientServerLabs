package network.client;

import network.TcpReceiver;
import protocol.DecryptorService;
import protocol.PacketDecoder;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class StoreClientTCP {

    private static final String HOST = "localhost";
    private static final int PORT = 8080;
    private static final int RECONNECT_DELAY_MS = 3000;

    private final byte clientId;
    private final byte[] key = "1234567890123456".getBytes();

    public StoreClientTCP(byte clientId) {
        this.clientId = clientId;
    }

    public void start() {
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
    }

    private void run(Socket socket) throws IOException {
        PacketDecoder decoder = new PacketDecoder(key);
        ResponseHandler responseHandler = new ResponseHandler();
        DecryptorService decryptorService = new DecryptorService(decoder, packet -> responseHandler.handle(packet));
        Consumer<byte[]> decryptorConsumer = decryptorService::decrypt;

        PacketGenerator generator = new PacketGenerator(clientId, key);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            pool.submit(() -> sendLoop(socket, generator));
            pool.submit(() -> {
                TcpReceiver receiver = new TcpReceiver(socket, decryptorConsumer);
                receiver.run();
            });
        }
    }

    private void sendLoop(Socket socket, PacketGenerator generator) {
        while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
            try {
                byte[] packet = generator.generate();
                socket.getOutputStream().write(packet);
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

