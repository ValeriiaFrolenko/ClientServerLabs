package protocol;

import model.Message;
import model.Packet;
import utils.AesUtil;
import utils.Crc16;

import java.nio.ByteBuffer;

public class PacketDecryptor{

    private final byte[] key;

    public PacketDecryptor(byte[] key) {
        this.key = key;
    }

    public Packet decrypt(byte[] data) throws Exception {
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

        String payload = AesUtil.decrypt(encryptedPayload, key);

        return new Packet(bSrc, bPktId, new Message(cType, bUserId, payload));
    }

    private void validateMagic(ByteBuffer buffer) {
        if (buffer.get() != PacketStructure.MAGIC_BYTE) {
            throw new IllegalArgumentException("Invalid magic byte");
        }
    }

    private void validateLength(byte[] data, int wLen) {
        if (data.length < PacketStructure.MIN_PACKET_SIZE + wLen) {
            throw new IllegalArgumentException("model.Packet too short");
        }
    }

    private void validateHeaderCrc(ByteBuffer buffer, byte[] data) {
        short headerCrc = buffer.getShort();
        if (headerCrc != Crc16.calculateCrc(data, 0, PacketStructure.HEADER_SIZE)) {
            throw new IllegalArgumentException("Header CRC mismatch");
        }
    }

    private byte[] readPayload(ByteBuffer buffer, int wLen) {
        byte[] encryptedPayload = new byte[wLen - MessageStructure.MESSAGE_HEADER_SIZE];
        buffer.get(encryptedPayload);
        return encryptedPayload;
    }

    private void validateMessageCrc(ByteBuffer buffer, byte[] data, int wLen) {
        short messageCrc = buffer.getShort();
        if (messageCrc != Crc16.calculateCrc(data, PacketStructure.OFFSET_MSG, wLen)) {
            throw new IllegalArgumentException("model.Message CRC mismatch");
        }
    }
}
