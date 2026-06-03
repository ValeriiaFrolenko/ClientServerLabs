package network.udp.server;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UdpConnectionManager implements AutoCloseable {

    private static final long TIMEOUT_SECONDS = 30;

    private record ClientRecord(InetSocketAddress address, long lastSeen) {}

    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentHashMap<Byte, ClientRecord> connections = new ConcurrentHashMap<>();

    public UdpConnectionManager() {
        cleaner.scheduleAtFixedRate(this::cleanup, TIMEOUT_SECONDS, TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public void ping(byte bSrc, InetSocketAddress address) {
        connections.put(bSrc, new ClientRecord(address, System.currentTimeMillis()));
    }

    public InetSocketAddress getAddress(byte bSrc) {
        ClientRecord record = connections.get(bSrc);
        return record != null ? record.address() : null;
    }

    @Override
    public void close() {
        cleaner.shutdown();
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        connections.entrySet().removeIf(entry ->
                now - entry.getValue().lastSeen() > TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS)
        );
    }
}