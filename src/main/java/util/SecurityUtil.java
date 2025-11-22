package util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class SecurityUtil {
    private static final String AES_MARKER = "AES";
    // A default key for game saves (Vault-Tec flavored)
    private static final String DEFAULT_SEED = "VAULT-TEC-TOP-SECRET-CLEARANCE";

    /**
     * Encrypts a raw byte array (serialized object) into a secure string.
     */
    public static String encrypt(byte[] data) throws Exception {
        return encrypt(data, DEFAULT_SEED);
    }

    public static String encrypt(byte[] data, String seed) throws Exception {
        byte[] key = deriveAesKey(seed);
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        byte[] cipherBytes = cipher.doFinal(data);

        ByteBuffer bb = ByteBuffer.allocate(iv.length + cipherBytes.length);
        bb.put(iv);
        bb.put(cipherBytes);

        return Base64.getEncoder().encodeToString(bb.array()) + "|" + AES_MARKER;
    }

    /**
     * Decrypts a secure string back into a raw byte array.
     */
    public static byte[] decrypt(String encryptedData) throws Exception {
        return decrypt(encryptedData, DEFAULT_SEED);
    }

    public static byte[] decrypt(String encryptedData, String seed) throws Exception {
        String[] parts = encryptedData.split("\\|", 2);
        if (parts.length < 2 || !parts[1].equals(AES_MARKER)) {
            throw new IllegalArgumentException("Invalid save file format.");
        }

        byte[] all = Base64.getDecoder().decode(parts[0]);
        byte[] iv = new byte[16];
        System.arraycopy(all, 0, iv, 0, 16);
        byte[] cipherBytes = new byte[all.length - 16];
        System.arraycopy(all, 16, cipherBytes, 0, cipherBytes.length);

        byte[] key = deriveAesKey(seed);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));

        return cipher.doFinal(cipherBytes);
    }

    private static byte[] deriveAesKey(String seed) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] digest = sha.digest(seed.getBytes(StandardCharsets.UTF_8));
        byte[] key = new byte[16];
        System.arraycopy(digest, 0, key, 0, 16);
        return key;
    }
}