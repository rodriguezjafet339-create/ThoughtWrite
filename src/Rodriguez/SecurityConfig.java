/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Rodriguez;

/**
 *
 * @author jafet
 */
import java.io.*;
import java.util.Base64;
import java.util.Properties;

public class SecurityConfig {

    private final File file;
    private final Properties props = new Properties();
    public static final String KEY_FAILED_ATTEMPTS = "failedAttempts";
    public static final String KEY_LOCKED_UNTIL = "lockedUntil";
    public static final String KEY_SALT = "salt";
    public static final String KEY_HASH = "hash";
    public static final String KEY_FIRST_RUN_DONE = "firstRunDone";

    public SecurityConfig(File file) throws IOException {
        this.file = file;
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
            }
        }
    }

    public void save() throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            props.store(out, "Secret Door Settings");
        }
    }

    // ---- password hash + salt ----
    public boolean isFirstRunDone() {
        return hasPassword() || Boolean.parseBoolean(props.getProperty(KEY_FIRST_RUN_DONE, "false"));
    }

    public void setFirstRunDone(boolean done) {
        props.setProperty(KEY_FIRST_RUN_DONE, String.valueOf(done));
    }

    public boolean hasPassword() {
        return props.getProperty(KEY_HASH) != null;
    }

    public byte[] getSalt() {
        return Base64.getDecoder().decode(props.getProperty(KEY_SALT));
    }

    public byte[] getHash() {
        return Base64.getDecoder().decode(props.getProperty(KEY_HASH));
    }

    public void setPassword(byte[] salt, byte[] hash) {
        props.setProperty(KEY_SALT, Base64.getEncoder().encodeToString(salt));
        props.setProperty(KEY_HASH, Base64.getEncoder().encodeToString(hash));
    }

    // ---- lockout state ----
    public int getFailedAttempts() {
        int attempts = Integer.parseInt(props.getProperty(KEY_FAILED_ATTEMPTS, "0"));
        return attempts;
    }

    public void setFailedAttempts(int n) {
        props.setProperty(KEY_FAILED_ATTEMPTS, String.valueOf(n));
    }

    public long getLockedUntil() {
        long lockedUntil = Long.parseLong(props.getProperty(KEY_LOCKED_UNTIL, "0"));
        return lockedUntil;
    }

    public void setLockedUntil(long t) {
        props.setProperty(KEY_LOCKED_UNTIL, String.valueOf(t));
    }

    public void clearPassword() {
        props.remove(KEY_SALT);
        props.remove(KEY_HASH);
        props.setProperty(KEY_FIRST_RUN_DONE, "true");   
        setFailedAttempts(0);
        setLockedUntil(0);
    }

}
