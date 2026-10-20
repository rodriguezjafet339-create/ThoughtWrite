package Rodriguez;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Plain .txt storage: one file per entry in the entries/ folder. */
public class EntryStore {

    private static final Logger LOG = Logger.getLogger(EntryStore.class.getName());
    private final Path dir;

    public EntryStore(String folder) {
        dir = Paths.get(folder);
        try {
            Files.createDirectories(dir);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /** All entries, oldest first. Files whose names aren't timestamps are skipped. */
    public List<Entry> loadAll() {
        List<Entry> list = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.txt")) {
            for (Path p : ds) {
                String name = p.getFileName().toString();
                String base = name.substring(0, name.length() - 4);
                try {
                    LocalDateTime t = LocalDateTime.parse(base, Entry.FILE_FMT);
                    list.add(new Entry(t, Files.readString(p, StandardCharsets.UTF_8)));
                } catch (DateTimeParseException | IOException ex) {
                    LOG.log(Level.WARNING, "Skipping " + name, ex);
                }
            }
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Could not read entries folder", ex);
        }
        list.sort(Comparator.comparing(Entry::getCreated));
        return list;
    }

    public void save(Entry e) {
        try {
            Files.writeString(dir.resolve(e.getFileName()), e.getText(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Could not save " + e.getFileName(), ex);
        }
    }

    public void delete(Entry e) {
        try {
            Files.deleteIfExists(dir.resolve(e.getFileName()));
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Could not delete " + e.getFileName(), ex);
        }
    }
}
