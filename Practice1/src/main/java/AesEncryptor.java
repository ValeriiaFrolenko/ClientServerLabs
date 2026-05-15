import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class AesEncryptor {

    public static byte[] encrypt(String plainText, byte[] key) throws Exception {
        if (plainText == null) {
            plainText = "";
        }
        if (key == null || key.length != 16) {
            throw new IllegalArgumentException("Key must be 16 bytes long for AES");
        }

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
        return cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
    }

    public static String decrypt(byte[] encryptedData, byte[] key) throws Exception {
        if (key == null || key.length != 16) {
            throw new IllegalArgumentException("Key must be 16 bytes long for AES");
        }

        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
        byte[] decrypted = cipher.doFinal(encryptedData);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}