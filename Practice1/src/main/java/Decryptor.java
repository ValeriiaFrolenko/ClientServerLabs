public interface Decryptor {
    Packet decrypt(byte[] encryptedPacket) throws Exception;
}
