import java.nio.ByteBuffer;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Encoder {

    public static byte[] encode(Packet packet, byte[] key) throws Exception {
        byte[] encryptedPayload = encrypt(packet.getbMsq().getMessage(), key);
        byte[] header = buildHeader(packet, encryptedPayload.length);
        byte[] message = buildMessage(packet.getbMsq(), encryptedPayload);
        return buildResult(header, message);
    }

    private static byte[] encrypt(String payload, byte[] key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
        return cipher.doFinal(payload.getBytes());
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
        ByteBuffer message = ByteBuffer.allocate(encryptedPayload.length + MessageStructure.MESSAGE_HEADER_SIZE);
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
}