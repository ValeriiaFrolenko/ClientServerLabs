package tcp_udp_protocol;

public interface Decryptor {
    void decrypt(byte[] encryptedPacket) throws Exception;
}
