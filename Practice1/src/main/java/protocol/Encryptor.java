package protocol;

import model.Packet;

public interface Encryptor {
    byte[] encrypt(Packet packet) throws Exception;
}
