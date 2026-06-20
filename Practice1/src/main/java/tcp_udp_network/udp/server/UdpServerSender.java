package tcp_udp_network.udp.server;

import tcp_udp_network.Sender;
import tcp_udp_protocol.PacketStructure;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.function.Consumer;

public class UdpServerSender implements Sender {

    private final UdpConnectionManager connectionManager;
    private final Consumer<DatagramPacket> onSend;

    public UdpServerSender(UdpConnectionManager connectionManager, Consumer<DatagramPacket> onSend) {
        this.connectionManager = connectionManager;
        this.onSend = onSend;
    }

    @Override
    public void sendMessage(byte[] packet) {
        byte dst = packet[PacketStructure.OFFSET_SRC];
        InetSocketAddress address = connectionManager.getAddress(dst);
        if (address == null) return;
        DatagramPacket datagram = new DatagramPacket(packet, packet.length, address);
        onSend.accept(datagram);
    }
}