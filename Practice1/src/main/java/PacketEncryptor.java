import java.nio.ByteBuffer;

public class PacketEncryptor implements Encryptor {

    private final byte[] key;

    public PacketEncryptor(byte[] key) {
        this.key = key;
    }

    public byte[] encrypt(Packet packet) throws Exception {
        if (packet == null) {
            throw new IllegalArgumentException("Packet cannot be null");
        }

        byte[] encryptedPayload = AesUtil.encrypt(packet.getbMsq().getMessage(), key);

        byte[] header = buildHeader(packet, encryptedPayload.length);
        byte[] message = buildMessage(packet.getbMsq(), encryptedPayload);

        return buildResult(header, message);
    }

    private byte[] buildHeader(Packet packet, int encryptedPayloadLength) {
        int wLen = encryptedPayloadLength + MessageStructure.MESSAGE_HEADER_SIZE;

        ByteBuffer header = ByteBuffer.allocate(PacketStructure.HEADER_SIZE);
        header.put(PacketStructure.MAGIC_BYTE);
        header.put(packet.getbSrc());
        header.putLong(packet.getbPktId());
        header.putInt(wLen);
        return header.array();
    }

    private byte[] buildMessage(Message msg, byte[] encryptedPayload) {
        ByteBuffer message = ByteBuffer.allocate(
                MessageStructure.MESSAGE_HEADER_SIZE + encryptedPayload.length);

        message.putInt(msg.getcType());
        message.putInt(msg.getbUserId());
        message.put(encryptedPayload);
        return message.array();
    }

    private byte[] buildResult(byte[] header, byte[] message) {
        ByteBuffer result = ByteBuffer.allocate(PacketStructure.MIN_PACKET_SIZE + message.length);

        result.put(header);
        result.putShort(Crc16.calculateCrc(header));
        result.put(message);
        result.putShort(Crc16.calculateCrc(message));

        return result.array();
    }
}
