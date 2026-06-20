package tcp_udp_network.udp;

import tcp_udp_core.Command;
import model.Message;
import model.Packet;
import tcp_udp_network.udp.client.ReliableUdpSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tcp_udp_protocol.PacketDecoder;
import tcp_udp_protocol.PacketEncoder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UdpReliabilityTest {

    private static final int PORT = 9091;
    private static final byte[] KEY = "1234567890123456".getBytes();
    private static final int DROP_FIRST_N = 3;

    private ExecutorService serverThread;
    private DatagramSocket serverSocket;

    @BeforeEach
    public void setUp() throws IOException {
        serverSocket = new DatagramSocket(PORT);
        serverThread = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    public void tearDown() {
        serverSocket.close();
        serverThread.shutdownNow();
    }

    @Test
    public void testRetryOnPacketLoss() throws Exception {
        int expectedDeliveries = 1;
        CountDownLatch deliveryLatch = new CountDownLatch(expectedDeliveries);
        AtomicInteger receivedCount = new AtomicInteger(0);

        serverThread.submit(() -> {
            int dropped = 0;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    byte[] buffer = new byte[4096];
                    DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
                    serverSocket.receive(datagram);

                    if (dropped < DROP_FIRST_N) {
                        dropped++;
                        continue;
                    }

                    receivedCount.incrementAndGet();
                    deliveryLatch.countDown();
                } catch (IOException e) {
                    break;
                }
            }
        });

        PacketEncoder encoder = new PacketEncoder(KEY);
        Message message = new Message(Command.GET_QUANTITY.ordinal(), 1, "Apple");
        Packet packet = new Packet((byte) 1, System.currentTimeMillis(), message);
        byte[] encoded = encoder.encode(packet);

        InetSocketAddress serverAddress = new InetSocketAddress("localhost", PORT);
        try (DatagramSocket clientSocket = new DatagramSocket();
             ReliableUdpSender sender = new ReliableUdpSender(clientSocket, serverAddress)) {

            sender.sendMessage(encoded);

            assertTrue(deliveryLatch.await(15, TimeUnit.SECONDS));
            assertTrue(receivedCount.get() >= 1);
        }
    }

    @Test
    public void testAcknowledgeStopsRetry() throws Exception {
        AtomicInteger receivedCount = new AtomicInteger(0);
        CountDownLatch firstDelivery = new CountDownLatch(1);

        serverThread.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    byte[] buffer = new byte[4096];
                    DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
                    serverSocket.receive(datagram);
                    receivedCount.incrementAndGet();
                    firstDelivery.countDown();
                } catch (IOException e) {
                    break;
                }
            }
        });

        PacketEncoder encoder = new PacketEncoder(KEY);
        Message message = new Message(Command.GET_QUANTITY.ordinal(), 1, "Apple");
        Packet packet = new Packet((byte) 1, System.currentTimeMillis(), message);
        byte[] encoded = encoder.encode(packet);

        PacketDecoder decoder = new PacketDecoder(KEY);
        Packet decoded = decoder.decode(encoded);
        long packetId = decoded.getbPktId();

        InetSocketAddress serverAddress = new InetSocketAddress("localhost", PORT);
        try (DatagramSocket clientSocket = new DatagramSocket();
             ReliableUdpSender sender = new ReliableUdpSender(clientSocket, serverAddress)) {

            sender.sendMessage(encoded);
            firstDelivery.await(5, TimeUnit.SECONDS);
            sender.acknowledge(packetId);

            int countAfterAck = receivedCount.get();
            Thread.sleep(7000);

            assertEquals(countAfterAck, receivedCount.get());
        }
    }
}