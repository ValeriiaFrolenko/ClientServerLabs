import org.junit.jupiter.api.Test;
import protocol.PacketStructure;

import static org.junit.jupiter.api.Assertions.*;

class PacketStructureTest {

    @Test
    void shouldHaveCorrectLengths() {
        assertEquals(1, PacketStructure.LEN_MAGIC);
        assertEquals(1, PacketStructure.LEN_SRC);
        assertEquals(8, PacketStructure.LEN_PKT_ID);
        assertEquals(4, PacketStructure.LEN_W_LEN);
        assertEquals(2, PacketStructure.LEN_CRC16);
    }

    @Test
    void shouldHaveCorrectOffsets() {
        assertEquals(0, PacketStructure.OFFSET_MAGIC);
        assertEquals(1, PacketStructure.OFFSET_SRC);
        assertEquals(2, PacketStructure.OFFSET_PKT_ID);
        assertEquals(10, PacketStructure.OFFSET_W_LEN);
        assertEquals(14, PacketStructure.OFFSET_HDR_CRC16);
        assertEquals(16, PacketStructure.OFFSET_MSG);
        assertEquals(14, PacketStructure.HEADER_SIZE);
        assertEquals(18, PacketStructure.MIN_PACKET_SIZE);
    }
}