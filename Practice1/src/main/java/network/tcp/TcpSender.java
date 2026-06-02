package network.tcp;

import network.Sender;
import network.tcp.server.ConnectionManager;

import java.io.IOException;
import java.net.Socket;

public class TcpSender implements Sender {

    private final ConnectionManager connectionManager;

    public TcpSender(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public void sendMessage(byte[] packet) {
        byte dst = packet[protocol.PacketStructure.OFFSET_SRC];
        Socket socket = connectionManager.getSocket(dst);
        if (socket == null) return;
        try {
            socket.getOutputStream().write(packet);
        } catch (IOException e) {
            System.err.println("Failed to send: " + e.getMessage());
        }
    }
}