/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Rodriguez;

/**
 *
 * @author jafet
 */
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordHasher {

    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;

    public static byte[] newSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public static byte[] hash(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return f.generateSecret(spec).getEncoded();
    }

    public static boolean verify(char[] attempt, byte[] salt, byte[] expected) throws Exception {
        byte[] actual = hash(attempt, salt);
        return MessageDigest.isEqual(actual, expected);
    }
}
