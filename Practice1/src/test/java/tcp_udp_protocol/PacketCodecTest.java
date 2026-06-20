package tcp_udp_protocol;

import model.Message;
import model.Packet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PacketCodecTest {

    Packet packet = createPacket();
    byte[] key = "1234567890abcdef".getBytes();
    private final PacketDecoder SUTDecr = new PacketDecoder(key);
    private final PacketEncoder SUTEncr = new PacketEncoder(key);


    private Packet createPacket() {
        Message message = new Message(1, 42, "Hello, World!");
        return new Packet((byte) 0x01, 123456789L, message);
    }

    @Test
    void shouldEncodeAndDecodePacketCorrectly() throws Exception {
        byte[] encoded = SUTEncr.encode(packet);
        Packet decoded = SUTDecr.decode(encoded);

        assertEquals(packet.getbSrc(), decoded.getbSrc());
        assertEquals(packet.getbPktId(), decoded.getbPktId());
        assertEquals(packet.getbMsq().getcType(), decoded.getbMsq().getcType());
        assertEquals(packet.getbMsq().getbUserId(), decoded.getbMsq().getbUserId());
        assertEquals(packet.getbMsq().getMessage(), decoded.getbMsq().getMessage());
    }
}