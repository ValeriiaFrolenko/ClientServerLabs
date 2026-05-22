package protocol;

import model.Packet;

import java.util.function.Consumer;

public class DecryptorService implements Decryptor {

    private final PacketDecoder packetDecoder;
    private final Consumer<Packet> onMessageReceived;

    public DecryptorService(PacketDecoder packetDecoder, Consumer<Packet> onMessageReceived) {
        this.packetDecoder = packetDecoder;
        this.onMessageReceived = onMessageReceived;
    }

    @Override
    public void decrypt(byte[] encryptedPacket) {
        try {
            Packet packet = packetDecoder.decode(encryptedPacket);
            onMessageReceived.accept(packet);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt packet", e);
        }
    }
}
