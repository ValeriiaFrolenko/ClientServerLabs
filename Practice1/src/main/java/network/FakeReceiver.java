package network;

import model.Message;
import model.Packet;
import protocol.Encryptor;
import protocol.PacketEncryptor;

import java.util.function.Consumer;

public class FakeReceiver implements Receiver, Runnable {

    private final Consumer<byte[]> onMessageReceived;
    private final byte[] key = "1234567890123456".getBytes();
    private final Encryptor encryptor = new PacketEncryptor(key);

    public FakeReceiver(Consumer<byte[]> onMessageReceived) {
        this.onMessageReceived = onMessageReceived;
    }

    @Override
    public void receiveMessage() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        byte[] fakePacket = createPacket();
        if (fakePacket.length > 0) {
            onMessageReceived.accept(fakePacket);
        }
    }

    private byte[] createPacket() {
        Message message = new Message(1, 42, "Apple");
        Packet packet = new Packet((byte) 0x01, 123456789L, message);
        try {
            return encryptor.encrypt(packet);
        } catch (Exception e) {
            return new byte[0];
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            receiveMessage();
        }
    }
}