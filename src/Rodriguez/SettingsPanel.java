package Rodriguez;

import java.awt.CardLayout;
import java.awt.Color;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Contents of the "Settings" tab. Switches between:
 *   none   - no password yet: only "Add Password"
 *   has    - change / delete
 *   verify - "enter your current password" (before change / delete)
 *   create - new password + confirm (used by add and by change)
 *
 * Talks to AuthService only; never touches the config directly.
 *
 * @author jafet
 */
public class SettingsPanel extends JPanel {

    private static final String NONE = "none";
    private static final String HAS = "has";
    private static final String VERIFY = "verify";
    private static final String CREATE = "create";

    private enum Pending { CHANGE, DELETE }

    private final AuthService auth;
    private final CardLayout cards = new CardLayout();
    private final CreatePasswordPanel createPanel;
    private final VerifyOldPasswordPanel verifyPanel;

    private Pending pending;        // what the verify step is unlocking
    private boolean changing;       // create panel is being used for "change" (vs "add")

    public SettingsPanel(AuthService auth) {
        this.auth = auth;
        setLayout(cards);
        setBackground(new Color(255, 255, 204));

        createPanel = new CreatePasswordPanel(this::savePassword, this::showOverview);
        verifyPanel = new VerifyOldPasswordPanel(auth, this::oldPasswordVerified,
                this::showOverview, this::showOverview);

        add(new SettingWOPassword(this::startAdd), NONE);
        add(new SettingsWithPasswordPanel(this::startChange, this::startDelete), HAS);
        add(verifyPanel, VERIFY);
        add(createPanel, CREATE);

        showOverview();
    }

    /** Back to the first screen: "Add Password" or "Change / Delete", depending on the config. */
    public final void showOverview() {
        createPanel.clear();
        cards.show(this, auth.hasPassword() ? HAS : NONE);
    }

    // -------------------------------------------------------------------- add

    private void startAdd() {                       // no old password needed: there isn't one
        changing = false;
        createPanel.setTitle("Create Password");
        createPanel.setSubmitText("Create");
        createPanel.clear();
        cards.show(this, CREATE);
    }

    // ----------------------------------------------------------------- change

    private void startChange() {
        pending = Pending.CHANGE;
        verifyPanel.reset("Change Password");
        cards.show(this, VERIFY);
    }

    // ----------------------------------------------------------------- delete

    private void startDelete() {
        pending = Pending.DELETE;
        verifyPanel.reset("Delete Password");
        cards.show(this, VERIFY);
    }

    // ------------------------------------------------------- after verify-old

    private void oldPasswordVerified() {
        if (pending == Pending.CHANGE) {
            changing = true;
            createPanel.setTitle("New Password");
            createPanel.setSubmitText("Save");
            createPanel.clear();
            cards.show(this, CREATE);
        } else {
            confirmAndDelete();
        }
    }

    private void confirmAndDelete() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete your password? ThoughtWrite will open without asking for one.",
                "Delete password", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            try {
                auth.removePassword();
                JOptionPane.showMessageDialog(this, "Password deleted.",
                        "ThoughtWrite", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                showError(ex);
            }
        }
        showOverview();
    }

    // ------------------------------------------------- new password submitted

    private void savePassword(char[] pw) {
        try {
            auth.setPassword(pw);
        } catch (Exception ex) {
            showError(ex);
            return;
        }
        JOptionPane.showMessageDialog(this, changing ? "Password changed." : "Password added.",
                "ThoughtWrite", JOptionPane.INFORMATION_MESSAGE);
        showOverview();
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, "Something went wrong: " + ex.getMessage(),
                "ThoughtWrite", JOptionPane.ERROR_MESSAGE);
    }
}
