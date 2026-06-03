package network.tcp;

import network.Receiver;
import network.tcp.server.TcpConnectionManager;
import protocol.PacketStructure;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class TcpReceiver implements Receiver, Runnable {

    private final Socket socket;
    private final Consumer<byte[]> onMessageReceived;
    private final TcpConnectionManager tcpConnectionManager;
    private Byte bSrc = null;
    private DataInputStream in;

    public TcpReceiver(Socket socket, Consumer<byte[]> onMessageReceived) {
        this(socket, onMessageReceived, null);
    }

    public TcpReceiver(Socket socket, Consumer<byte[]> onMessageReceived, TcpConnectionManager tcpConnectionManager) {
        this.socket = socket;
        this.onMessageReceived = onMessageReceived;
        this.tcpConnectionManager = tcpConnectionManager;
    }

    @Override
    public void receiveMessage() throws IOException {
        byte[] header = readPacket(PacketStructure.HEADER_SIZE);
        int wLen = ByteBuffer.wrap(header).getInt(PacketStructure.OFFSET_W_LEN);
        int restSize = PacketStructure.MIN_PACKET_SIZE + wLen - PacketStructure.HEADER_SIZE;
        byte[] rest = readPacket(restSize);
        byte[] packet = ByteBuffer.allocate(header.length + rest.length)
                .put(header)
                .put(rest)
                .array();
        registerSource(header);
        onMessageReceived.accept(packet);
    }

    private void registerSource(byte[] header) {
        if (tcpConnectionManager == null) return;
        if (bSrc == null) {
            bSrc = header[PacketStructure.OFFSET_SRC];
            tcpConnectionManager.register(bSrc, socket);
        } else if (bSrc != header[PacketStructure.OFFSET_SRC]) {
            throw new IllegalArgumentException("Source address mismatch for the same connection.");
        }
    }

    private byte[] readPacket(int size) throws IOException {
        byte[] packet = in.readNBytes(size);
        if (packet.length < size) {
            throw new IOException("Connection closed while reading packet.");
        }
        return packet;
    }

    @Override
    public void run() {
        try (socket; DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            this.in = dis;
            while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                try {
                    receiveMessage();
                } catch (SocketTimeoutException _) {
                } catch (IOException | IllegalArgumentException e) {
                    System.err.println("TcpReceiver error: " + e.getMessage());
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("TcpReceiver failed to open/close stream: " + e.getMessage());
        } finally {
            if (bSrc != null && tcpConnectionManager != null) {
                tcpConnectionManager.unregister(bSrc);
            }
        }
    }
}