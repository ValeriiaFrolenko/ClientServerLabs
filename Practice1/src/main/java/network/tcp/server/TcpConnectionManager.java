package network.tcp.server;

import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class TcpConnectionManager {

    private final ConcurrentHashMap<Byte, Socket> connections = new ConcurrentHashMap<>();

    public void register(byte bSrc, Socket socket) {
        connections.put(bSrc, socket);
    }

    public void unregister(byte bSrc) {
        connections.remove(bSrc);
    }

    public Socket getSocket(byte bSrc) {
        return connections.get(bSrc);
    }
}