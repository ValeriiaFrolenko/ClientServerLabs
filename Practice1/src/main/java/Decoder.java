import java.nio.ByteBuffer;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Decoder {

    public static Packet decode(byte[] data, byte[] key) throws Exception {
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
        String payload = decrypt(encryptedPayload, key);
        return new Packet(bSrc, bPktId, new Message(cType, bUserId, payload));
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

    private static String decrypt(byte[] encryptedPayload, byte[] key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
        return new String(cipher.doFinal(encryptedPayload));
    }
}