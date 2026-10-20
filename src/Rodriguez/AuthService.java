/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Rodriguez;

/**
 *
 * @author jafet
 */
public class AuthService {

    private static final int MAX_ATTEMPTS = 3;
    private static final long COOLDOWN_MS = 60_000;

    private final SecurityConfig config;

    public AuthService(SecurityConfig config) {
        this.config = config;
    }

    public boolean isFirstRunDone() {
        return config.isFirstRunDone();
    }

    public void skipPassword() throws Exception {
        config.setFirstRunDone(true);
        config.save();
    }

    public boolean hasPassword() {
        return config.hasPassword();
    }

    public void setPassword(char[] password) throws Exception {
        byte[] salt = PasswordHasher.newSalt();
        byte[] hash = PasswordHasher.hash(password, salt);
        config.setPassword(salt, hash);
        config.setFirstRunDone(true);
        config.save();
    }

    // If a lock exists but its time has passed, wipe it.
    private void clearExpiredLock() throws Exception {
        long until = config.getLockedUntil();
        if (until != 0 && until <= System.currentTimeMillis()) {
            config.setFailedAttempts(0);
            config.setLockedUntil(0);
            config.save();
        }
    }

    public boolean isLockedOut() throws Exception {
        clearExpiredLock();
        if (config.getLockedUntil() > System.currentTimeMillis()) {
            return true;
        }
        return false;
    }

    public long secondsLeft() {
        long msLeft = config.getLockedUntil() - System.currentTimeMillis();
        return Math.max(0, (msLeft + 999) / 1000);
    }

    public int attemptsLeft() {
        long until = config.getLockedUntil();
        if (until != 0 && until <= System.currentTimeMillis()) {
            return MAX_ATTEMPTS;   // cooldown is over, so a fresh set of tries
        }
        return Math.max(0, MAX_ATTEMPTS - config.getFailedAttempts());
    }

    // Returns true if the password was right.
    public boolean tryUnlock(char[] attempt) throws Exception {
        if (!config.hasPassword()) {
            return false;              // nothing to unlock; avoid the NPE
        }
        if (isLockedOut()) {
            return false;                  // locked: don't even check, don't count it
        }
        boolean ok = PasswordHasher.verify(attempt, config.getSalt(), config.getHash());
        if (ok) {
            config.setFailedAttempts(0);
            config.setLockedUntil(0);
            config.save();
        } else {
            int attempts = config.getFailedAttempts() + 1;   // read the saved count, add one
            config.setFailedAttempts(attempts);              // store the new count

            if (attempts >= MAX_ATTEMPTS) {
                config.setLockedUntil(System.currentTimeMillis() + COOLDOWN_MS);
            }

            config.save();
        }
        return ok;
    }

    //-----------changing/deleting/adding the password-----------------//
    public boolean verifyOldPassword(char[] attempt) throws Exception {
        if (!config.hasPassword()) {
            return false;
        }
        return PasswordHasher.verify(attempt, config.getSalt(), config.getHash());
    }

    //just incase ill need this
    /*public boolean changePassword(char[] oldPw, char[] newPw) throws Exception {
        if (!verifyOldPassword(oldPw)) {
            return false;
        }
        setPassword(newPw);
        return true;
    }*/
    public void removePassword() throws Exception {
        config.clearPassword();
        config.save();
    }
}
