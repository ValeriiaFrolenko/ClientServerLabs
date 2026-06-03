import network.tcp.client.StoreClientTCP;
import network.tcp.server.StoreServerTCP;

public class TcpApp {
    public static void main(String[] args) throws InterruptedException {
        Thread serverThread = new Thread(() -> new StoreServerTCP().start());
        serverThread.start();
        Thread.sleep(500);
        for (byte i = 1; i <= 3; i++) {
            byte clientId = i;
            new Thread(() -> new StoreClientTCP(clientId).start()).start();
        }
    }
}