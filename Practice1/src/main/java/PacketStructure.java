public final class PacketStructure {

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