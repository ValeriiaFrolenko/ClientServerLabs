package network.tcp.server;

import network.Sender;

import java.io.IOException;
import java.net.Socket;

public class TcpServerSender implements Sender {

    private final TcpConnectionManager tcpConnectionManager;

    public TcpServerSender(TcpConnectionManager tcpConnectionManager) {
        this.tcpConnectionManager = tcpConnectionManager;
    }

    @Override
    public void sendMessage(byte[] packet) {
        byte dst = packet[protocol.PacketStructure.OFFSET_SRC];
        Socket socket = tcpConnectionManager.getSocket(dst);
        if (socket == null) return;
        try {
            socket.getOutputStream().write(packet);
        } catch (IOException e) {
            System.err.println("Failed to send: " + e.getMessage());
        }
    }
}