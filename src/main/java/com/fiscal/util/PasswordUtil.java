package com.fiscal.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

/**
 * Enterprise cryptographic password utility using PBKDF2WithHmacSHA256.
 * Completely cross-platform, zero native dependencies, FIPS-compliant.
 */
public final class PasswordUtil {

    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256; // 32 bytes
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {}

    /**
     * Generates a 16-byte random salt encoded as 32 hex characters.
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return bytesToHex(salt);
    }

    /**
     * Hashes a plaintext password with the provided salt using PBKDF2WithHmacSHA256.
     */
    public static String hashPassword(String password, String saltHex) {
        if (password == null || saltHex == null) {
            throw new IllegalArgumentException("Password and salt cannot be null");
        }
        try {
            byte[] salt = hexToBytes(saltHex);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Cryptographic algorithm unavailable", e);
        }
    }

    public static String hashPasswordWithSalt(String password) {
        String salt = generateSalt();
        return salt + ":" + hashPassword(password, salt);
    }

    /**
     * Verifies a candidate password against a stored hash (which may contain embedded 'salt:hash' or use separate salt).
     */
    public static boolean verifyPassword(String candidatePassword, String storedHash, String salt) {
        if (candidatePassword == null || storedHash == null) {
            return false;
        }
        if (storedHash.contains(":")) {
            String[] parts = storedHash.split(":", 2);
            salt = parts[0];
            storedHash = parts[1];
        }
        if (salt == null) return false;
        String calculated = hashPassword(candidatePassword, salt);
        return slowEquals(calculated, storedHash);
    }

    public static boolean verifyPassword(String candidatePassword, String storedHash) {
        return verifyPassword(candidatePassword, storedHash, null);
    }

    private static boolean slowEquals(String a, String b) {
        byte[] aBytes = a.getBytes();
        byte[] bBytes = b.getBytes();
        int diff = aBytes.length ^ bBytes.length;
        for (int i = 0; i < aBytes.length && i < bBytes.length; i++) {
            diff |= aBytes[i] ^ bBytes[i];
        }
        return diff == 0;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
