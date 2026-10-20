package Rodriguez;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

/**
 * "Enter your current password" step shown before Change / Delete.
 * 2 tries; after the 2nd wrong one it calls onTooManyFailures (no penalty, no lockout).
 * Hand-written (no Form Editor), styled to match the other panels.
 *
 * @author jafet
 */
public class VerifyOldPasswordPanel extends JPanel {

    private static final int MAX_TRIES = 2;
    private static final Color PAPER = new Color(255, 255, 204);

    private final AuthService auth;
    private final Runnable onSuccess;
    private final Runnable onCancel;
    private final Runnable onTooManyFailures;

    private final JLabel heading = new JLabel("Enter your password");
    private final JPasswordField passwordField = new JPasswordField(24);
    private int triesLeft = MAX_TRIES;

    public VerifyOldPasswordPanel(AuthService auth, Runnable onSuccess,
            Runnable onCancel, Runnable onTooManyFailures) {
        this.auth = auth;
        this.onSuccess = onSuccess;
        this.onCancel = onCancel;
        this.onTooManyFailures = onTooManyFailures;
        buildUi();
    }

    /** Call before showing the panel. Sets the heading, clears the field, restores 2 tries. */
    public void reset(String title) {
        heading.setText(title);
        passwordField.setText("");
        triesLeft = MAX_TRIES;
        SwingUtilities.invokeLater(passwordField::requestFocusInWindow);
    }

    private void buildUi() {
        setBackground(PAPER);
        setLayout(new GridBagLayout());

        heading.setFont(new Font("Courier New", Font.PLAIN, 14));
        JLabel prompt = new JLabel("Enter your current password to continue");
        prompt.setFont(new Font("Courier New", Font.PLAIN, 12));

        JButton continueButton = new JButton("Continue");
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setBackground(new Color(255, 204, 204));
        continueButton.setBackground(new Color(255, 255, 153));
        continueButton.addActionListener(e -> submit());
        passwordField.addActionListener(e -> submit());
        cancelButton.addActionListener(e -> {
            passwordField.setText("");
            triesLeft = MAX_TRIES;
            onCancel.run();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancelButton);
        buttons.add(continueButton);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;

        c.gridy = 0;
        c.insets = new Insets(17, 23, 4, 23);
        add(heading, c);

        c.gridy = 1;
        c.insets = new Insets(2, 23, 8, 23);
        add(new JSeparator(), c);

        c.gridy = 2;
        c.insets = new Insets(4, 23, 4, 23);
        add(prompt, c);

        c.gridy = 3;
        add(passwordField, c);

        c.gridy = 4;
        c.insets = new Insets(10, 23, 4, 23);
        add(buttons, c);

        JPanel filler = new JPanel();       // pushes everything to the top
        filler.setOpaque(false);
        c.gridy = 5;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        add(filler, c);
    }

    private void submit() {
        char[] pw = passwordField.getPassword();
        try {
            if (auth.verifyOldPassword(pw)) {
                passwordField.setText("");
                triesLeft = MAX_TRIES;
                onSuccess.run();
                return;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Something went wrong: " + ex.getMessage(),
                    "ThoughtWrite", JOptionPane.ERROR_MESSAGE);
            return;
        } finally {
            Arrays.fill(pw, '\0');
        }

        // wrong password
        passwordField.setText("");
        triesLeft--;
        if (triesLeft <= 0) {
            JOptionPane.showMessageDialog(this, "Wrong password. Going back to Settings.",
                    "Wrong password", JOptionPane.WARNING_MESSAGE);
            triesLeft = MAX_TRIES;
            onTooManyFailures.run();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Wrong password. " + triesLeft + (triesLeft == 1 ? " try left." : " tries left."),
                    "Wrong password", JOptionPane.WARNING_MESSAGE);
            passwordField.requestFocusInWindow();
        }
    }
}
