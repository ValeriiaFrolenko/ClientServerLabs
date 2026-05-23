package protocol;

import model.Packet;
import java.util.function.Consumer;

public class EncryptorService implements Encryptor {

    private final PacketEncoder encoder;
    private final Consumer<byte[]> onMessageEncrypted;

    public EncryptorService(PacketEncoder encoder, Consumer<byte[]> onMessageEncrypted) {
        this.encoder = encoder;
        this.onMessageEncrypted = onMessageEncrypted;
    }

    @Override
    public byte[] encrypt(Packet packet) {
        byte[] encodedData;
        try {
            encodedData = encoder.encode(packet);
            onMessageEncrypted.accept(encodedData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return encodedData;
    }
}