package tcp_udp_network.udp.client;

import tcp_udp_network.Sender;
import tcp_udp_protocol.PacketStructure;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReliableUdpSender implements Sender, AutoCloseable {

    private static final long TIMEOUT_SECONDS = 3;

    private record PendingRequest(byte[] packet, long sentAt) {}

    private final DatagramSocket socket;
    private final InetSocketAddress serverAddress;
    private final ConcurrentHashMap<Long, PendingRequest> pending = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public ReliableUdpSender(DatagramSocket socket, InetSocketAddress serverAddress) {
        this.socket = socket;
        this.serverAddress = serverAddress;
        scheduler.scheduleAtFixedRate(this::checkTimeouts, TIMEOUT_SECONDS, TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void sendMessage(byte[] data) {
        send(data);
        long packetId = ByteBuffer.wrap(data, PacketStructure.OFFSET_PKT_ID, 8).getLong();
        pending.put(packetId, new PendingRequest(data, System.currentTimeMillis()));
    }

    public void acknowledge(long packetId) {
        pending.remove(packetId);
    }

    @Override
    public void close() {
        scheduler.shutdown();
    }

    private void send(byte[] data) {
        try {
            socket.send(new DatagramPacket(data, data.length, serverAddress));
        } catch (IOException e) {
            System.err.println("Send failed: " + e.getMessage());
        }
    }

    private void checkTimeouts() {
        long now = System.currentTimeMillis();
        pending.forEach((packetId, request) -> {
            if (now - request.sentAt() > TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS)) {
                pending.put(packetId, new PendingRequest(request.packet(), System.currentTimeMillis()));
                send(request.packet());
            }
        });
    }
}