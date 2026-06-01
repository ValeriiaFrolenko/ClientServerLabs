import network.client.StoreClientTCP;
import network.server.StoreServerTCP;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Thread serverThread = new Thread(() -> {
            try {
                new StoreServerTCP().start();
            } catch (Exception e) {
                System.err.println("Server error: " + e.getMessage());
            }
        });
        serverThread.start();

        Thread.sleep(500);
        for (byte i = 1; i <= 3; i++) {
            byte clientId = i;
            new Thread(() -> new StoreClientTCP(clientId).start()).start();
        }
    }
}