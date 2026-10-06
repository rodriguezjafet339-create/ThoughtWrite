package com.mycompany.trialslang;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** One journal page. The creation timestamp is the hidden key (and the filename). */
public class Entry {

    static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DATELINE_FMT = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter JUMP_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy \u00B7 h:mm a", Locale.ENGLISH);

    private final LocalDateTime created;
    private String text;

    public Entry(LocalDateTime created, String text) {
        this.created = created.withNano(0);
        this.text = text == null ? "" : text;
    }

    public LocalDateTime getCreated() { return created; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text == null ? "" : text; }

    public String getFileName() { return FILE_FMT.format(created) + ".txt"; }
    public String getDateline() { return DATELINE_FMT.format(created).toLowerCase(Locale.ENGLISH); }
    public String getJumpLabel() { return JUMP_FMT.format(created); }

    public boolean isBlank() { return text.trim().isEmpty(); }

    /** Case-insensitive match against the body text or the dateline. */
    public boolean matches(String query) {
        String q = query.toLowerCase(Locale.ENGLISH);
        return text.toLowerCase(Locale.ENGLISH).contains(q) || getDateline().contains(q);
    }
}
