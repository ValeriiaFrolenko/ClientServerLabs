package protocol;

import model.Packet;

public interface Decryptor {
    Packet decrypt(byte[] encryptedPacket) throws Exception;
}
