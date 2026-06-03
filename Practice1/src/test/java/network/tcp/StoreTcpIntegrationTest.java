package network.tcp;

import network.tcp.client.StoreClientTCP;
import network.tcp.server.StoreServerTCP;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoreTcpIntegrationTest {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Test
    void testTcpClientReconnectBehaviorWithoutServer() throws InterruptedException {
        StoreClientTCP client = new StoreClientTCP((byte) 5);
        CountDownLatch latch = new CountDownLatch(1);

        executor.submit(() -> {
            try {
                client.start();
            } finally {
                latch.countDown();
            }
        });

        boolean terminated = latch.await(4, TimeUnit.SECONDS);
        assertFalse(terminated);
    }

    @Test
    void testTcpServerAndClientInteraction() throws InterruptedException, IOException {
        StoreServerTCP server = new StoreServerTCP();
        StoreClientTCP client = new StoreClientTCP((byte) 1);

        executor.submit(server::start);
        Thread.sleep(500);

        try (Socket socket = new Socket("localhost", 8080)) {
            assertTrue(socket.isConnected());
        }

        executor.submit(client::start);
        Thread.sleep(3000);
    }
}