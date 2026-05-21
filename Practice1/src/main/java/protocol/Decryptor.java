package protocol;

import model.Packet;

public interface Decryptor {
    void decrypt(byte[] encryptedPacket) throws Exception;
}
