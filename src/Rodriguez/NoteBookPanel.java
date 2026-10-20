package Rodriguez;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.Timer;

/**
 * Controller for the notebook: owns the entry list, wires the three panels
 * together, and handles autosave, search, jump-to-date, navigation and delete.
 *
 * @author jafet
 */
public class NoteBookPanel extends javax.swing.JPanel {

    private static final int AUTOSAVE_DELAY_MS = 800;

    private final EntryStore store = new EntryStore(
        new java.io.File(System.getProperty("user.home"), ".thoughtwrite" + java.io.File.separator + "entries").getPath());
    private final SearchRowPanel searchRow = new SearchRowPanel();
    private final PageCardPanel pageCard = new PageCardPanel();
    private final NavRowPanel navRow = new NavRowPanel();

    private final Timer saveTimer = new Timer(AUTOSAVE_DELAY_MS, e -> saveCurrent());

    private List<Entry> all = new ArrayList<>();      // every entry, oldest -> newest
    private List<Entry> visible = new ArrayList<>();  // entries matching the search box
    private final List<Entry> jumpTargets = new ArrayList<>(); // dropdown rows, newest first
    private Entry current;                            // page on screen (null = no matches)
    private int index;                                // position of current within visible

    /**
     * Creates new form NoteBookPanel
     */
    public NoteBookPanel() {
        initComponents();

        setLayout(new java.awt.BorderLayout());

        add(searchRow, java.awt.BorderLayout.NORTH);
        add(pageCard, java.awt.BorderLayout.CENTER);
        add(navRow, java.awt.BorderLayout.SOUTH);

        saveTimer.setRepeats(false);

        // wiring
        pageCard.addTextChangeListener(saveTimer::restart);          // debounced autosave
        searchRow.addSearchListener(this::applyFilter);
        searchRow.addJumpListener(i -> goTo(jumpTargets.get(i)));
        searchRow.addDeleteListener(this::deletePage);
        navRow.setOnPrev(this::prevPage);
        navRow.setOnNext(this::nextPage);

        // open the notebook
        all = store.loadAll();
        if (all.isEmpty() || !all.get(all.size() - 1).getCreated().toLocalDate().equals(LocalDate.now())) {
            all.add(new Entry(LocalDateTime.now(), ""));   // fresh page for today
        }
        visible = new ArrayList<>(all);
        display(visible.get(visible.size() - 1));
    }

    /**
     * Saves any pending edit immediately. Call before the app exits.
     */
    public void flush() {
        saveCurrent();
    }

    // ------------------------------------------------------------------ saving
    private void saveCurrent() {
        saveTimer.stop();
        if (current == null) {
            return;
        }
        current.setText(pageCard.getText());
        if (current.isBlank()) {
            store.delete(current);      // never keep empty files around
        } else {
            store.save(current);
        }
        updateChrome();
    }

    // ------------------------------------------------------------- navigation
    private void prevPage() {
        if (current != null && index > 0) {
            goTo(visible.get(index - 1));
        }
    }

    private void nextPage() {
        if (current == null) {
            return;
        }
        if (index < visible.size() - 1) {
            goTo(visible.get(index + 1));
        } else if (canStartNewPage()) {
            saveCurrent();
            openBlankPage();               // turn to a fresh page
        }
    }

    /**
     * Save the page we're leaving, drop it if it was left blank, then show the
     * target.
     */
    private void goTo(Entry target) {
        saveCurrent();
        if (current != null && current != target && current.isBlank()) {
            all.remove(current);
            visible.remove(current);
        }
        display(target);
    }

    private void openBlankPage() {
        Entry e = new Entry(LocalDateTime.now(), "");
        all.add(e);
        visible.add(e);
        display(e);
        pageCard.focusText();
    }

    private boolean canStartNewPage() {
        return searchRow.getQuery().trim().isEmpty() && current != null && !current.isBlank();
    }

    // ---------------------------------------------------------------- display
    private void display(Entry e) {
        current = e;
        index = visible.indexOf(e);
        pageCard.setEditable(true);
        pageCard.setText(e.getText());
        updateChrome();
    }

    private void showEmpty() {
        current = null;
        pageCard.setText("");
        pageCard.setEditable(false);
        pageCard.setDateline("no matching pages");
        navRow.setPrevEnabled(false);
        navRow.setNextEnabled(false);
        refreshJump();
    }

    /**
     * Dateline, page counter, arrow states and dropdown.
     */
    private void updateChrome() {
        if (current == null) {
            return;
        }
        pageCard.setDateline(current.getDateline() + " \u00B7 " + (index + 1) + "/" + visible.size());
        navRow.setPrevEnabled(index > 0);
        navRow.setNextEnabled(index < visible.size() - 1 || canStartNewPage());
        refreshJump();
    }

    private void refreshJump() {
        jumpTargets.clear();
        List<String> labels = new ArrayList<>();
        for (int i = visible.size() - 1; i >= 0; i--) {   // newest first, like the mockup
            Entry e = visible.get(i);
            if (!e.isBlank()) {
                jumpTargets.add(e);
                labels.add(e.getJumpLabel());
            }
        }
        searchRow.setJumpItems(labels);
    }

    // ----------------------------------------------------------------- search
    private void applyFilter(String raw) {
        saveCurrent();
        Entry keep = current;
        if (keep != null && keep.isBlank()) {   // an untouched blank page isn't worth keeping
            all.remove(keep);
            keep = null;
        }

        String q = raw.trim();
        visible = new ArrayList<>();
        for (Entry e : all) {
            if (q.isEmpty() || e.matches(q)) {
                visible.add(e);
            }
        }

        if (visible.isEmpty()) {
            if (q.isEmpty()) {
                openBlankPage();
            } else {
                showEmpty();
            }
            return;
        }
        display(visible.contains(keep) ? keep : visible.get(visible.size() - 1));
    }

    // ----------------------------------------------------------------- delete
    private void deletePage() {
        if (current == null || pageCard.getText().trim().isEmpty()) {
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Tear out this page? This can't be undone.",
                "Tear out page",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        saveTimer.stop();
        store.delete(current);
        int i = index;
        all.remove(current);
        visible.remove(current);
        current = null;

        if (!visible.isEmpty()) {
            display(visible.get(Math.min(i, visible.size() - 1)));
        } else if (searchRow.getQuery().trim().isEmpty()) {
            openBlankPage();
        } else {
            showEmpty();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
