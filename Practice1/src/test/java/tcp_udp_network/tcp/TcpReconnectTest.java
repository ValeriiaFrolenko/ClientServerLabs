package tcp_udp_network.tcp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TcpReconnectTest {

    private static final int PORT = 9090;
    private static final String HOST = "localhost";

    private final ExecutorService testPool = Executors.newCachedThreadPool();

    @AfterEach
    public void tearDown() {
        testPool.shutdownNow();
    }

    @Test
    public void testClientReconnectsAfterServerRestart() throws Exception {
        CountDownLatch serverReadyLatch = new CountDownLatch(1);
        CountDownLatch firstConnectionLatch = new CountDownLatch(1);
        CountDownLatch reconnectLatch = new CountDownLatch(1);
        AtomicInteger connectionCount = new AtomicInteger(0);

        testPool.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                serverReadyLatch.countDown();
                Socket client = serverSocket.accept();
                connectionCount.incrementAndGet();
                firstConnectionLatch.countDown();
                Thread.sleep(500);
                client.close();
            } catch (Exception e) {
                System.err.println("First server error: " + e.getMessage());
            }
        });

        assertTrue(serverReadyLatch.await(5, TimeUnit.SECONDS));
        Thread clientThread = startReconnectingClient();

        assertTrue(firstConnectionLatch.await(5, TimeUnit.SECONDS));
        Thread.sleep(1000);

        testPool.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                Socket client = serverSocket.accept();
                connectionCount.incrementAndGet();
                reconnectLatch.countDown();
                client.close();
            } catch (Exception e) {
                System.err.println("Second server error: " + e.getMessage());
            }
        });

        assertTrue(reconnectLatch.await(10, TimeUnit.SECONDS));
        assertTrue(connectionCount.get() >= 2);
        clientThread.interrupt();
    }

    private Thread startReconnectingClient() {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try (Socket socket = new Socket(HOST, PORT)) {
                    socket.getInputStream().read();
                } catch (IOException e) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
        thread.start();
        return thread;
    }
}