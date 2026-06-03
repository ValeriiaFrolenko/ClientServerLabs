package network.udp.client;

import network.PacketGenerator;
import network.ResponseHandler;
import network.udp.UdpReceiver;
import protocol.DecryptorService;
import protocol.EncryptorService;
import protocol.PacketDecoder;
import protocol.PacketEncoder;
import model.Packet;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class StoreClientUDP {

    private static final String HOST = "localhost";
    private static final int PORT = 8081;
    private static final int BUFFER_SIZE = 4096;

    private final byte clientId;
    private final byte[] key = "1234567890123456".getBytes();
    private final InetSocketAddress serverAddress = new InetSocketAddress(HOST, PORT);

    private final ExecutorService encryptorPool = Executors.newFixedThreadPool(2);
    private final ExecutorService senderPool = Executors.newFixedThreadPool(2);
    private final ExecutorService decryptorPool = Executors.newFixedThreadPool(2);
    private final ExecutorService responsePool = Executors.newFixedThreadPool(2);

    public StoreClientUDP(byte clientId) {
        this.clientId = clientId;
    }

    public void start() {
        PacketGenerator generator = new PacketGenerator(clientId);
        PacketDecoder decoder = new PacketDecoder(key);
        PacketEncoder encoder = new PacketEncoder(key);

        try (DatagramSocket socket = new DatagramSocket();
             ExecutorService pool = Executors.newFixedThreadPool(2);
             ReliableUdpSender sender = new ReliableUdpSender(socket, serverAddress)) {

            Consumer<byte[]> senderConsumer = data -> senderPool.submit(() -> sender.sendMessage(data));

            EncryptorService encryptorService = new EncryptorService(encoder, senderConsumer);
            Consumer<Packet> encryptorConsumer = packet -> encryptorPool.submit(() -> encryptorService.encrypt(packet));

            ResponseHandler responseHandler = new ResponseHandler(packet ->
                    responsePool.submit(() -> {
                        System.out.println("Response: " + packet.getbMsq().getMessage());
                        sender.acknowledge(packet.getbPktId());
                    }));

            DecryptorService decryptorService = new DecryptorService(decoder, responseHandler::handle);
            Consumer<byte[]> decryptorConsumer = data -> decryptorPool.submit(() -> decryptorService.decrypt(data));

            pool.submit(() -> sendLoop(socket, generator, encryptorConsumer));
            pool.submit(() -> receiveLoop(socket, decryptorConsumer));

        } catch (IOException e) {
            System.err.println("Client failed: " + e.getMessage());
        } finally {
            stop();
        }
    }

    private void stop() {
        encryptorPool.shutdown();
        senderPool.shutdown();
        decryptorPool.shutdown();
        responsePool.shutdown();
    }

    private void sendLoop(DatagramSocket socket, PacketGenerator generator, Consumer<Packet> encryptorConsumer) {
        while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
            try {
                encryptorConsumer.accept(generator.generate());
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("Send failed: " + e.getMessage());
                break;
            }
        }
    }

    private void receiveLoop(DatagramSocket socket, Consumer<byte[]> decryptorConsumer) {
        while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
            try {
                byte[] buffer = new byte[BUFFER_SIZE];
                DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
                socket.receive(datagram);
                new UdpReceiver(datagram, decryptorConsumer).receive();
            } catch (IOException e) {
                System.err.println("Receive failed: " + e.getMessage());
                break;
            }
        }
    }
}