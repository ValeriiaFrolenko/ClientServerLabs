package network;

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
    private final ConnectionManager connectionManager;
    private Byte bSrc = null;
    DataInputStream in;


    public TcpReceiver(Socket socket, Consumer<byte[]> onMessageReceived, ConnectionManager connectionManager) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.onMessageReceived = onMessageReceived;
        this.connectionManager = connectionManager;
    }

    @Override
    public void receiveMessage() throws IOException {
        byte[] header = readPacket(PacketStructure.HEADER_SIZE);
        int wLen = ByteBuffer.wrap(header).getInt(PacketStructure.OFFSET_W_LEN);
        int restSize = PacketStructure.MIN_PACKET_SIZE + wLen - PacketStructure.HEADER_SIZE;
        byte[] rest  = readPacket(restSize);
        byte[] packet = ByteBuffer.allocate(header.length + rest.length)
                .put(header)
                .put(rest)
                .array();
        registerSocket(header);
        onMessageReceived.accept(packet);
    }

    private void registerSocket(byte[] header) {
        if (bSrc == null) {
            bSrc = header[PacketStructure.OFFSET_SRC];
            connectionManager.register(bSrc, socket);
        } else if (bSrc != header[PacketStructure.OFFSET_SRC]) {
            throw new IllegalArgumentException("Source byte mismatch for the same connection.");
        }
    }

    private byte[] readPacket(int size) throws IOException {
        byte[] packet = in.readNBytes(size);
        if (packet.length < size) {
            throw new IOException("Client disconnected abruptly while sending payload.");
        }
        return packet;
    }

    @Override
    public void run() {
        try (socket) {
            while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                try {
                    receiveMessage();
                } catch (SocketTimeoutException _) {
                } catch (IOException | IllegalArgumentException e) {
                    System.err.println("Receiver thread caught an error: " + e.getMessage());
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to release socket resources: " + e.getMessage());
        } finally {
            if (bSrc != null) {
                connectionManager.unregister(bSrc);
            }
        }
    }
}