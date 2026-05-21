public final class PacketStructure {

    /**
     * Packet Structure:
     * - Magic Byte (1 byte)
     * - Source (1 byte)
     * - Packet ID (8 bytes)
     * - Payload Length (4 bytes)
     * - Header CRC16 (2 bytes)
     * - Message (variable length)
     * - Message CRC16 (2 bytes)
     * Packet Offsets:
     * - Magic Byte: Offset 0
     * - Source: Offset 1
     * - Packet ID: Offset 2
     * - Payload Length: Offset 10
     * - Header CRC16: Offset 14
     * - Message: Offset 16
     * Header Size: 14 bytes (Magic Byte + Source + Packet ID + Payload Length)
     * Minimum Packet Size: 18 bytes (Header  + Header CRC16 + Message CRC16 with empty payload)
     */

    private PacketStructure() {}

    public static final byte MAGIC_BYTE = 0x13;

    public static final int LEN_MAGIC = 1;
    public static final int LEN_SRC = 1;
    public static final int LEN_PKT_ID = 8;
    public static final int LEN_W_LEN = 4;
    public static final int LEN_CRC16 = 2;

    public static final int OFFSET_MAGIC = 0;
    public static final int OFFSET_SRC = OFFSET_MAGIC + LEN_MAGIC;
    public static final int OFFSET_PKT_ID = OFFSET_SRC + LEN_SRC;
    public static final int OFFSET_W_LEN = OFFSET_PKT_ID + LEN_PKT_ID;
    public static final int OFFSET_HDR_CRC16 = OFFSET_W_LEN + LEN_W_LEN;
    public static final int OFFSET_MSG = OFFSET_HDR_CRC16 + LEN_CRC16;

    public static final int HEADER_SIZE = OFFSET_HDR_CRC16;
    public static final int MIN_PACKET_SIZE = OFFSET_MSG + LEN_CRC16;
}