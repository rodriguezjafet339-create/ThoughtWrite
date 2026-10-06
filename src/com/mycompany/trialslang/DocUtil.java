package com.mycompany.trialslang;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Tiny helper: one Runnable for insert / remove / change. */
final class DocUtil {
    private DocUtil() {}

    static DocumentListener of(Runnable r) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { r.run(); }
            @Override public void removeUpdate(DocumentEvent e) { r.run(); }
            @Override public void changedUpdate(DocumentEvent e) { r.run(); }
        };
    }
}
