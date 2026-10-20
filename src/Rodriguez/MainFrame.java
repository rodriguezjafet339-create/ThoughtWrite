package Rodriguez;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.io.File;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * Thin shell. Two top-level cards: cover - first run, create password, lock
 * screen (sub-cards) main - tabs: Journal (NoteBookPanel) and Settings Nothing
 * in the journal is created until the cover is dismissed.
 *
 * @author jafet
 */
public class MainFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(MainFrame.class.getName());

    private static final Color PAPER = new Color(255, 255, 204);

    private static final String CARD_COVER = "cover";
    private static final String CARD_MAIN = "main";
    private static final String COVER_FIRST_RUN = "firstRun";
    private static final String COVER_CREATE = "create";
    private static final String COVER_LOCK = "lock";

    private final AuthService auth;
    private final CardLayout rootCards = new CardLayout();
    private final CardLayout coverCards = new CardLayout();
    private final JPanel root = new JPanel(rootCards);
    private final JPanel cover = new JPanel(coverCards);

    private final CreatePasswordPanel firstRunCreate;
    private final LockScreen lockScreen;
    private NoteBookPanel notebook;          // created only after the cover is dismissed

    /**
     * Creates new form MainFrame
     */
    public MainFrame() {
        initComponents();

        setTitle("ThoughtWrite");
        auth = openAuth();

        // ---- cover: first run / create / lock
        FirstRun firstRun = new FirstRun(this::onFirstRunYes, this::onFirstRunNo);
        firstRunCreate = new CreatePasswordPanel(this::onFirstRunPasswordCreated,
                () -> coverCards.show(cover, COVER_FIRST_RUN));
        lockScreen = new LockScreen(auth, this::showMain);

        cover.setBackground(PAPER);
        cover.add(centered(firstRun), COVER_FIRST_RUN);
        cover.add(centered(firstRunCreate), COVER_CREATE);
        cover.add(centered(lockScreen), COVER_LOCK);
        root.add(cover, CARD_COVER);

        setContentPane(root);
        setSize(480, 640);
        setResizable(false);
        setLocationRelativeTo(null);

        // Flush any pending autosave before the app exits.
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (notebook != null) {
                    notebook.flush();
                }
            }
        });

        // ---- startup flow
        if (!auth.isFirstRunDone()) {
            coverCards.show(cover, COVER_FIRST_RUN);
        } else if (auth.hasPassword()) {
            showLock();
        } else {
            showMain();
        }
    }

    // ------------------------------------------------------------- config / auth
    /**
     * ~/.thoughtwrite/config.properties, or whatever -Dthoughtwrite.config=...
     * points at. Use the override while developing so a bug can't lock you out
     * of the real config.
     */
    private static File configFile() {
        String override = System.getProperty("thoughtwrite.config");
        if (override != null && !override.isBlank()) {
            return new File(override);
        }
        File dir = new File(System.getProperty("user.home"), ".thoughtwrite");
        return new File(dir, "config.properties");
    }

    private static AuthService openAuth() {
        File file = configFile();
        try {
            File dir = file.getAbsoluteFile().getParentFile();
            if (dir != null) {
                dir.mkdirs();
            }
            return new AuthService(new SecurityConfig(file));
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(null,
                    "Couldn't read the settings file:\n" + file + "\n\n" + ex.getMessage(),
                    "ThoughtWrite", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return null;   // not reached
        }
    }

    // --------------------------------------------------------------- navigation
    /**
     * Wraps a fixed-size panel so it sits in the middle of the window.
     */
    private static JPanel centered(JPanel inner) {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setBackground(PAPER);
        wrap.add(inner);
        return wrap;
    }

    private void showLock() {
        lockScreen.refresh();
        coverCards.show(cover, COVER_LOCK);
        rootCards.show(root, CARD_COVER);
    }

    private void showMain() {
        if (notebook == null) {
            notebook = new NoteBookPanel();
            SettingsPanel settings = new SettingsPanel(auth);

            JTabbedPane tabs = new JTabbedPane();

            tabs.setFont(new java.awt.Font("Courier New", java.awt.Font.PLAIN, 12));
            tabs.setBackground(new java.awt.Color(255, 255, 204)); 
            tabs.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
                @Override
                protected void installDefaults() {
                    super.installDefaults();
                    tabAreaInsets = new java.awt.Insets(4, 6, 0, 6);
                    contentBorderInsets = new java.awt.Insets(0, 0, 0, 0);
                    tabInsets = new java.awt.Insets(6, 18, 6, 18);   // top, left, bottom, right padding inside each tab
                }

                @Override
                protected void paintTabBackground(java.awt.Graphics g, int placement, int index,
                        int x, int y, int w, int h, boolean selected) {
                    g.setColor(selected ? new java.awt.Color(255, 255, 204) // selected tab = page color
                            : new java.awt.Color(240, 225, 180));  // other tab = darker
                    g.fillRect(x, y, w, h);
                }

                @Override
                protected void paintTabBorder(java.awt.Graphics g, int placement, int index,
                        int x, int y, int w, int h, boolean selected) {
                    g.setColor(new java.awt.Color(201, 155, 107));
                    g.drawRect(x, y, w, h);
                }

                @Override
                protected void paintContentBorder(java.awt.Graphics g, int placement, int selectedIndex) {
                }

                @Override
                protected void paintFocusIndicator(java.awt.Graphics g, int placement, java.awt.Rectangle[] rects,
                        int index, java.awt.Rectangle iconRect, java.awt.Rectangle textRect, boolean selected) {
                }
            });
            tabs.addTab("Journal", notebook);
            tabs.addTab("Settings", settings);
            // Always land on the overview when the Settings tab is opened.
            tabs.addChangeListener(e -> {
                if (tabs.getSelectedComponent() == settings) {
                    settings.showOverview();
                }
            });
            root.add(tabs, CARD_MAIN);
        }
        rootCards.show(root, CARD_MAIN);
    }

    // ---------------------------------------------------------- first-run flow
    private void onFirstRunYes() {
        firstRunCreate.clear();
        coverCards.show(cover, COVER_CREATE);
    }

    private void onFirstRunNo() {
        try {
            auth.skipPassword();            // saves firstRunDone=true, no password
        } catch (Exception ex) {
            showError(ex);
            return;
        }
        showMain();
    }

    private void onFirstRunPasswordCreated(char[] pw) {
        try {
            auth.setPassword(pw);           // saves the password and firstRunDone=true
        } catch (Exception ex) {
            showError(ex);
            return;
        }
        showMain();                         // they just typed it; no need to lock straight away
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, "Something went wrong: " + ex.getMessage(),
                "ThoughtWrite", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new MainFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
