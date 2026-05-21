public final class MessageStructure {

    /**
     * Message Structure:
     * - Command Type (4 bytes)
     * - User ID (4 bytes)
     * - Payload (variable length)
     * Message Offsets:
     * - Command Type: Offset 0
     * - User ID: Offset 4
     * - Payload: Offset 8
     * Message Header Size: 8 bytes (Command Type + User ID)
     */

    private MessageStructure() {}

    public static final int LEN_CMD_TYPE = 4;
    public static final int LEN_USER_ID = 4;

    public static final int OFFSET_CMD_TYPE = 0;
    public static final int OFFSET_USER_ID = OFFSET_CMD_TYPE + LEN_CMD_TYPE;
    public static final int OFFSET_PAYLOAD = OFFSET_USER_ID + LEN_USER_ID;

    public static final int MESSAGE_HEADER_SIZE = OFFSET_PAYLOAD;
}