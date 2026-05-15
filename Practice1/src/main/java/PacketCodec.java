import java.nio.ByteBuffer;

public class PacketCodec {

    public static byte[] encode(Packet packet, byte[] key) throws Exception {
        if (packet == null) {
            throw new IllegalArgumentException("Packet cannot be null");
        }

        byte[] encryptedPayload = AesEncryptor.encrypt(packet.getbMsq().getMessage(), key);

        byte[] header = buildHeader(packet, encryptedPayload.length);
        byte[] message = buildMessage(packet.getbMsq(), encryptedPayload);

        return buildResult(header, message);
    }

    public static Packet decode(byte[] data, byte[] key) throws Exception {
        if (data == null || data.length < PacketStructure.MIN_PACKET_SIZE) {
            throw new IllegalArgumentException("Invalid packet data");
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);

        validateMagic(buffer);
        byte bSrc = buffer.get();
        long bPktId = buffer.getLong();
        int wLen = buffer.getInt();

        validateLength(data, wLen);
        validateHeaderCrc(buffer, data);

        int cType = buffer.getInt();
        int bUserId = buffer.getInt();

        byte[] encryptedPayload = readPayload(buffer, wLen);
        validateMessageCrc(buffer, data, wLen);

        String payload = AesEncryptor.decrypt(encryptedPayload, key);

        return new Packet(bSrc, bPktId, new Message(cType, bUserId, payload));
    }

    private static byte[] buildHeader(Packet packet, int encryptedPayloadLength) {
        int wLen = encryptedPayloadLength + MessageStructure.MESSAGE_HEADER_SIZE;

        ByteBuffer header = ByteBuffer.allocate(PacketStructure.HEADER_SIZE);
        header.put(PacketStructure.MAGIC_BYTE);
        header.put(packet.getbSrc());
        header.putLong(packet.getbPktId());
        header.putInt(wLen);
        return header.array();
    }

    private static byte[] buildMessage(Message msg, byte[] encryptedPayload) {
        ByteBuffer message = ByteBuffer.allocate(
                MessageStructure.MESSAGE_HEADER_SIZE + encryptedPayload.length);

        message.putInt(msg.getcType());
        message.putInt(msg.getbUserId());
        message.put(encryptedPayload);
        return message.array();
    }

    private static byte[] buildResult(byte[] header, byte[] message) {
        ByteBuffer result = ByteBuffer.allocate(PacketStructure.MIN_PACKET_SIZE + message.length);

        result.put(header);
        result.putShort(Crc16.calculateCrc(header));
        result.put(message);
        result.putShort(Crc16.calculateCrc(message));

        return result.array();
    }

    private static void validateMagic(ByteBuffer buffer) {
        if (buffer.get() != PacketStructure.MAGIC_BYTE) {
            throw new IllegalArgumentException("Invalid magic byte");
        }
    }

    private static void validateLength(byte[] data, int wLen) {
        if (data.length < PacketStructure.MIN_PACKET_SIZE + wLen) {
            throw new IllegalArgumentException("Packet too short");
        }
    }

    private static void validateHeaderCrc(ByteBuffer buffer, byte[] data) {
        short headerCrc = buffer.getShort();
        if (headerCrc != Crc16.calculateCrc(data, 0, PacketStructure.HEADER_SIZE)) {
            throw new IllegalArgumentException("Header CRC mismatch");
        }
    }

    private static byte[] readPayload(ByteBuffer buffer, int wLen) {
        byte[] encryptedPayload = new byte[wLen - MessageStructure.MESSAGE_HEADER_SIZE];
        buffer.get(encryptedPayload);
        return encryptedPayload;
    }

    private static void validateMessageCrc(ByteBuffer buffer, byte[] data, int wLen) {
        short messageCrc = buffer.getShort();
        if (messageCrc != Crc16.calculateCrc(data, PacketStructure.OFFSET_MSG, wLen)) {
            throw new IllegalArgumentException("Message CRC mismatch");
        }
    }
}