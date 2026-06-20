package tcp_udp_protocol;

import model.Packet;

public interface Encryptor {
    byte[] encrypt(Packet packet) throws Exception;
}
