package network.tcp.client;

import network.Sender;

import java.io.IOException;
import java.net.Socket;

public class TcpClientSender implements Sender {

    private final Socket socket;

    public TcpClientSender(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void sendMessage(byte[] data) {
        try {
            socket.getOutputStream().write(data);
        } catch (IOException e) {
            System.err.println("Send failed: " + e.getMessage());
        }
    }
}