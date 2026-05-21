package protocol;

import model.Packet;

import java.util.function.Consumer;

public class DecryptorImp implements Decryptor {

    private final PacketDecryptor packetDecryptor;
    private final Consumer<Packet> onMessageReceived;

    public DecryptorImp(PacketDecryptor packetDecryptor, Consumer<Packet> onMessageReceived) {
        this.packetDecryptor = packetDecryptor;
        this.onMessageReceived = onMessageReceived;
    }

    @Override
    public void decrypt(byte[] encryptedPacket) {
        try {
            Packet packet = packetDecryptor.decrypt(encryptedPacket);
            onMessageReceived.accept(packet);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt packet", e);
        }
    }
}
