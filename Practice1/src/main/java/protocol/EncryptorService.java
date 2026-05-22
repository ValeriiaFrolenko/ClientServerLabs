package protocol;

import model.Packet;
import java.util.function.Consumer;

public class EncryptorService {

    private final PacketEncoder encoder;
    private final Consumer<byte[]> onMessageEncrypted;

    public EncryptorService(PacketEncoder encoder, Consumer<byte[]> onMessageEncrypted) {
        this.encoder = encoder;
        this.onMessageEncrypted = onMessageEncrypted;
    }

    public void encryptMessage(Packet packet) {
        try {
            byte[] encodedData = encoder.encode(packet);
            onMessageEncrypted.accept(encodedData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}