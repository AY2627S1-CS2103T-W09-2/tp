package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.AddressBook;
import seedu.address.model.UserPrefs;

public class SandboxStorageTest {
    @TempDir
    public Path folder;

    @Test
    public void confinedStorage_roundTripsWithoutExternalFiles() throws Exception {
        Path home = Files.createDirectory(folder.resolve("home"));
        JsonAddressBookStorage roster = new JsonAddressBookStorage(home.resolve("data/roster.json"), home);
        JsonUserPrefsStorage prefs = new JsonUserPrefsStorage(home.resolve("preferences.json"), home);
        roster.saveAddressBook(new AddressBook());
        prefs.saveUserPrefs(new UserPrefs());
        assertEquals(new AddressBook(), roster.readAddressBook().orElseThrow());
        assertEquals(new UserPrefs(), prefs.readUserPrefs().orElseThrow());
        try (var paths = Files.walk(home)) {
            assertTrue(paths.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test
    public void confinedStorage_rejectsOutsideReadsAndWritesEvenViaOverloads() throws Exception {
        Path home = Files.createDirectory(folder.resolve("home"));
        Path outside = Files.writeString(folder.resolve("outside.json"), "preserve this sentinel");
        JsonAddressBookStorage roster = new JsonAddressBookStorage(home.resolve("roster.json"), home);
        JsonUserPrefsStorage prefs = new JsonUserPrefsStorage(outside, home);
        assertThrows(DataLoadingException.class, () -> roster.readAddressBook(outside));
        assertThrows(IOException.class, () -> roster.saveAddressBook(new AddressBook(), outside));
        assertThrows(DataLoadingException.class, prefs::readUserPrefs);
        assertThrows(IOException.class, () -> prefs.saveUserPrefs(new UserPrefs()));
        assertEquals("preserve this sentinel", Files.readString(outside));
    }

    @Test
    public void confinedStorage_rechecksRedirectsAfterConstruction() throws Exception {
        Path home = Files.createDirectory(folder.resolve("home"));
        Path outside = Files.createDirectory(folder.resolve("outside"));
        Path sentinel = Files.writeString(outside.resolve("preferences.json"), "external sentinel");
        JsonUserPrefsStorage prefs = new JsonUserPrefsStorage(home.resolve("preferences.json"), home);
        JsonAddressBookStorage roster = new JsonAddressBookStorage(home.resolve("data/roster.json"), home);
        try {
            Files.createSymbolicLink(home.resolve("preferences.json"), sentinel);
            Files.createSymbolicLink(home.resolve("data"), outside);
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Symbolic links unavailable: " + e);
        }
        assertThrows(DataLoadingException.class, prefs::readUserPrefs);
        assertThrows(IOException.class, () -> prefs.saveUserPrefs(new UserPrefs()));
        assertThrows(DataLoadingException.class, roster::readAddressBook);
        assertThrows(IOException.class, () -> roster.saveAddressBook(new AddressBook()));
        assertEquals("external sentinel", Files.readString(sentinel));
        try (var paths = Files.list(outside)) {
            assertEquals(1, paths.count());
        }
    }
}
