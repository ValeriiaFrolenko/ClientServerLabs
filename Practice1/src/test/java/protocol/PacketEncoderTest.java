package protocol;

import model.Message;
import model.Packet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PacketEncoderTest {

    private final Packet packet = createPacket();
    private final byte[] key = "1234567890abcdef".getBytes();
    private final PacketEncoder SUT = new PacketEncoder(key);

    private Packet createPacket() {
        Message message = new Message(1, 42, "Hello, World!");
        return new Packet((byte) 0x01, 123456789L, message);
    }

    @Test
    void shouldThrowExceptionForNullPacketEncode() {
        assertThrows(IllegalArgumentException.class, () -> SUT.encode(null));
    }

    @Test
    void shouldProduceSameEncodedBytesForSamePacket() throws Exception {
        byte[] encoded1 = SUT.encode(packet);
        byte[] encoded2 = SUT.encode(packet);
        assertArrayEquals(encoded1, encoded2);
    }
}