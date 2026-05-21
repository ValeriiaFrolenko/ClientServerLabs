import org.junit.jupiter.api.Test;

import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class PacketCodecTest {

    Packet packet = createPacket();
    byte[] key = "1234567890abcdef".getBytes();
    private final Decryptor SUTDecr = new PacketDecryptor(key);
    private final Encryptor SUTEncr = new PacketEncryptor(key);


    private Packet createPacket() {
        Message message = new Message(1, 42, "Hello, World!");
        return new Packet((byte) 0x01, 123456789L, message);
    }

    @Test
    void shouldEncodeAndDecodePacketCorrectly() throws Exception {
        byte[] encoded = SUTEncr.encrypt(packet);
        Packet decoded = SUTDecr.decrypt(encoded);

        assertEquals(packet.getbSrc(), decoded.getbSrc());
        assertEquals(packet.getbPktId(), decoded.getbPktId());
        assertEquals(packet.getbMsq().getcType(), decoded.getbMsq().getcType());
        assertEquals(packet.getbMsq().getbUserId(), decoded.getbMsq().getbUserId());
        assertEquals(packet.getbMsq().getMessage(), decoded.getbMsq().getMessage());
    }
}