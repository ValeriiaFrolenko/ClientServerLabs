package util;

import org.junit.jupiter.api.Test;
import utils.AesUtil;

import static org.junit.jupiter.api.Assertions.*;

class AesUtilTest {

    private final byte[] key = "1234567890abcdef".getBytes();
    private final String plainText = "Hello, World!";

    @Test
    void shouldReturnSameTextAfterEncryptionAndDecryption() throws Exception {
        byte[] encrypted = AesUtil.encrypt(plainText, key);
        String decrypted = AesUtil.decrypt(encrypted, key);
        assertEquals(plainText, decrypted);
    }

    @Test
    void shouldHandleEmptyString() throws Exception {
        byte[] encrypted = AesUtil.encrypt("", key);
        String decrypted = AesUtil.decrypt(encrypted, key);
        assertEquals("", decrypted);
    }

    @Test
    void shouldHandleNullString() throws Exception {
        byte[] encrypted = AesUtil.encrypt(null, key);
        String decrypted = AesUtil.decrypt(encrypted, key);
        assertEquals("", decrypted);
    }

    @Test
    void shouldThrowExceptionForInvalidKey() {
        byte[] invalidKey = "shortkey".getBytes();
        assertThrows(IllegalArgumentException.class, () -> AesUtil.encrypt(plainText, invalidKey));
        assertThrows(IllegalArgumentException.class, () -> AesUtil.decrypt(new byte[16], invalidKey));
    }

    @Test
    void shouldThrowExceptionForNullKey() {
        assertThrows(IllegalArgumentException.class, () -> AesUtil.encrypt(plainText, null));
        assertThrows(IllegalArgumentException.class, () -> AesUtil.decrypt(new byte[16], null));
    }

    @Test
    void shouldProduceSameCiphertextForSamePlaintext() throws Exception {
        byte[] encrypted1 = AesUtil.encrypt(plainText, key);
        byte[] encrypted2 = AesUtil.encrypt(plainText, key);
        assertArrayEquals(encrypted1, encrypted2);
    }

    @Test
    void shouldProduceDifferentBytesFromPlaintext() throws Exception {
        byte[] encrypted = AesUtil.encrypt(plainText, key);
        assertFalse(java.util.Arrays.equals(plainText.getBytes(), encrypted));
    }

    @Test
    void shouldNotDecryptWithWrongKey() throws Exception {
        byte[] encrypted = AesUtil.encrypt(plainText, key);
        byte[] wrongKey = "fedcba0987654321".getBytes();
        assertThrows(Exception.class, () -> AesUtil.decrypt(encrypted, wrongKey));
    }
}