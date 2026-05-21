package protocol;

public interface Decryptor {
    void decrypt(byte[] encryptedPacket) throws Exception;
}
