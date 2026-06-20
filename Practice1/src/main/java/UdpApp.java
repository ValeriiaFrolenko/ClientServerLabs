
import tcp_udp_network.udp.client.StoreClientUDP;
import tcp_udp_network.udp.server.StoreServerUDP;

public class UdpApp {
    public static void main(String[] args) throws InterruptedException {
        Thread serverThread = new Thread(() -> new StoreServerUDP().start());
        serverThread.start();
        Thread.sleep(500);
        for (byte i = 1; i <= 3; i++) {
            byte clientId = i;
            new Thread(() -> new StoreClientUDP(clientId).start()).start();
        }
    }
}
