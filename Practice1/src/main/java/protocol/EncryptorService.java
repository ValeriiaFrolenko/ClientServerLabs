package protocol;

import model.Packet;
import java.util.function.Consumer;

public class EncryptorService {

    private final Encryptor encryptor;
    private final Consumer<byte[]> onMessageEncrypted;

    public EncryptorService(Encryptor encryptor, Consumer<byte[]> onMessageEncrypted) {
        this.encryptor = encryptor;
        this.onMessageEncrypted = onMessageEncrypted;
    }

    public void encryptMessage(Packet packet) {
        try {
            byte[] encryptedData = encryptor.encrypt(packet);
            onMessageEncrypted.accept(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}