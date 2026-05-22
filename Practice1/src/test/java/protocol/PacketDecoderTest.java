package protocol;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class PacketDecoderTest {

    private final byte[] key = "1234567890abcdef".getBytes();
    private final byte[] encoded = new byte[] {19, 1, 0, 0, 0, 0, 7, 91, -51, 21, 0, 0, 0, 24, -91, -90, 0, 0, 0, 1, 0, 0, 0, 42, -91, -40, 45, -38, -123, -9, 73, -43, -14, -126, -102, 25, 18, 5, -92, -115, 100, -94};
    private final PacketDecoder SUT = new PacketDecoder(key);

    @Test
    void shouldThrowExceptionForNullPacketDecode() {
        assertThrows(IllegalArgumentException.class, () -> SUT.decode(null));
    }

    @Test
    void shouldThrowExceptionForInvalidPacketData() {
        byte[] invalidData = new byte[5];
        assertThrows(IllegalArgumentException.class, () -> SUT.decode(invalidData));
    }

    @Test
    void shouldThrowExceptionForWrongMagicByte(){
        byte[] encoded = this.encoded;
        byte wrongByte = 0x00;
        encoded[0] = wrongByte;
        assertThrows(IllegalArgumentException.class, () -> SUT.decode(encoded));
    }

    @Test
    void shouldThrowExceptionForWrongLength() {
        byte[] encoded = this.encoded;
        int wrongLength = 9999;
        ByteBuffer.wrap(encoded).position(10).putInt(wrongLength);
        assertThrows(IllegalArgumentException.class, () -> SUT.decode(encoded));
    }

    @Test
    void shouldThrowExceptionForWrongHeaderCrc() {
        byte[] encoded = this.encoded;
        encoded[PacketStructure.OFFSET_HDR_CRC16] ^= (byte) 0xFF;
        assertThrows(IllegalArgumentException.class, () -> SUT.decode(encoded));
    }

    @Test
    void shouldThrowExceptionForWrongMessageCrc() {
        byte[] encoded = this.encoded;
        int wLen = ByteBuffer.wrap(encoded).getInt(PacketStructure.OFFSET_W_LEN);
        int msgCrcOffset = PacketStructure.OFFSET_MSG + wLen;
        encoded[msgCrcOffset] ^= (byte) 0xFF;
        assertThrows(IllegalArgumentException.class, () -> SUT.decode(encoded));
    }
}