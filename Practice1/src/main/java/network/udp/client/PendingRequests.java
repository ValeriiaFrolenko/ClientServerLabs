package network.udp.client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PendingRequests implements AutoCloseable {

    private static final long TIMEOUT_SECONDS = 3;

    private record PendingRequest(byte[] packet, long sentAt) {}

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentHashMap<Long, PendingRequest> pending = new ConcurrentHashMap<>();
    private final Consumer<byte[]> onRetry;

    public PendingRequests(Consumer<byte[]> onRetry) {
        this.onRetry = onRetry;
        scheduler.scheduleAtFixedRate(this::checkTimeouts, TIMEOUT_SECONDS, TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public void add(long packetId, byte[] packet) {
        pending.put(packetId, new PendingRequest(packet, System.currentTimeMillis()));
    }

    public void acknowledge(long packetId) {
        pending.remove(packetId);
    }

    @Override
    public void close() {
        scheduler.shutdown();
    }

    private void checkTimeouts() {
        long now = System.currentTimeMillis();
        pending.forEach((packetId, request) -> {
            if (now - request.sentAt() > TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS)) {
                pending.put(packetId, new PendingRequest(request.packet(), System.currentTimeMillis()));
                onRetry.accept(request.packet());
            }
        });
    }
}