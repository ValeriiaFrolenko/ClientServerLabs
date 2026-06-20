package tcp_udp_network.udp;

import tcp_udp_network.udp.server.UdpConnectionManager;
import tcp_udp_protocol.PacketStructure;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.function.Consumer;

public class UdpReceiver {

    private final DatagramPacket datagram;
    private final UdpConnectionManager connectionManager;
    private final Consumer<byte[]> onMessageReceived;

    public UdpReceiver(DatagramPacket datagram, UdpConnectionManager connectionManager, Consumer<byte[]> onMessageReceived) {
        this.datagram = datagram;
        this.connectionManager = connectionManager;
        this.onMessageReceived = onMessageReceived;
    }

    public UdpReceiver(DatagramPacket datagram, Consumer<byte[]> onMessageReceived) {
        this.datagram = datagram;
        this.connectionManager = null;
        this.onMessageReceived = onMessageReceived;
    }

    public void receive() {
        byte[] data = new byte[datagram.getLength()];
        System.arraycopy(datagram.getData(), 0, data, 0, datagram.getLength());
        byte bSrc = data[PacketStructure.OFFSET_SRC];
        InetSocketAddress address = new InetSocketAddress(datagram.getAddress(), datagram.getPort());
        if (connectionManager != null) {
            connectionManager.ping(bSrc, address);
        }
        onMessageReceived.accept(data);
    }
}