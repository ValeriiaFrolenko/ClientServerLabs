import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class PacketCodecTest {

    Packet packet = createPacket();
    byte[] key = "1234567890abcdef".getBytes();


    private Packet createPacket() {
        Message message = new Message(1, 42, "Hello, World!");
        return new Packet((byte) 0x01, 123456789L, message);
    }

    @Test
    void shouldEncodeAndDecodePacketCorrectly() throws Exception {
        byte[] encoded = PacketCodec.encode(packet, key);
        Packet decoded = PacketCodec.decode(encoded, key);

        assertEquals(packet.getbSrc(), decoded.getbSrc());
        assertEquals(packet.getbPktId(), decoded.getbPktId());
        assertEquals(packet.getbMsq().getcType(), decoded.getbMsq().getcType());
        assertEquals(packet.getbMsq().getbUserId(), decoded.getbMsq().getbUserId());
        assertEquals(packet.getbMsq().getMessage(), decoded.getbMsq().getMessage());
    }

    @Test
    void shouldThrowExceptionForNullPacketEncode() {
        assertThrows(IllegalArgumentException.class, () -> PacketCodec.encode(null, key));
    }

    @Test
    void shouldThrowExceptionForNullPacketDecode() {
        assertThrows(IllegalArgumentException.class, () -> PacketCodec.decode(null, key));
    }

    @Test
    void shouldThrowExceptionForInvalidPacketData() {
        byte[] invalidData = new byte[5];
        assertThrows(IllegalArgumentException.class, () -> PacketCodec.decode(invalidData, key));
    }

    @Test
    void shouldThrowExceptionForWrongMagicByte() throws Exception {
        byte wrongByte = 0x00;
        byte[] encoded = PacketCodec.encode(packet, key);
        encoded[0] = wrongByte;
        assertThrows(IllegalArgumentException.class, () -> PacketCodec.decode(encoded, key));
    }

    @Test
    void shouldThrowExceptionForWrongLength() throws Exception {
        byte[] encoded = PacketCodec.encode(packet, key);
        int wrongLength = 9999;
        ByteBuffer.wrap(encoded).position(10).putInt(wrongLength);
        assertThrows(IllegalArgumentException.class, () -> PacketCodec.decode(encoded, key));
    }

    @Test
    void shouldThrowExceptionForWrongHeaderCrc() throws Exception {
        byte[] encoded = PacketCodec.encode(packet, key);
        encoded[PacketStructure.OFFSET_HDR_CRC16] ^= (byte) 0xFF;
        assertThrows(IllegalArgumentException.class, () -> PacketCodec.decode(encoded, key));
    }

    @Test
    void shouldThrowExceptionForWrongMessageCrc() throws Exception {
        byte[] encoded = PacketCodec.encode(packet, key);
        int wLen = ByteBuffer.wrap(encoded).getInt(PacketStructure.OFFSET_W_LEN);
        int msgCrcOffset = PacketStructure.HEADER_SIZE + wLen - PacketStructure.LEN_CRC16;
        encoded[msgCrcOffset] ^= (byte) 0xFF;
        assertThrows(IllegalArgumentException.class, () -> PacketCodec.decode(encoded, key));
    }

    @Test
    void shouldProduceSameEncodedBytesForSamePacket() throws Exception {
        byte[] encoded1 = PacketCodec.encode(packet, key);
        byte[] encoded2 = PacketCodec.encode(packet, key);
        assertArrayEquals(encoded1, encoded2);
    }
}