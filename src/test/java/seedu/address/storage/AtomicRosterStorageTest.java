package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.util.JsonUtil;
import seedu.address.model.AddressBook;
import seedu.address.model.ReadOnlyAddressBook;

public class AtomicRosterStorageTest {
    @TempDir
    public Path folder;

    @Test
    public void save_partialSerializationFailure_preservesFileAndRemovesTemporaryFile() throws Exception {
        Path target = folder.resolve("roster.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(target) {
            @Override
            protected void writeRoster(ReadOnlyAddressBook roster, Path temporary) throws IOException {
                Files.writeString(temporary, "partial JSON");
                throw new IOException("serialization failed");
            }
        };
        assertThrows(IOException.class, () -> storage.saveAddressBook(getTypicalAddressBook()));
        assertFalse(Files.exists(target));
        try (var files = Files.list(folder)) {
            assertEquals(0, files.count());
        }
        Files.writeString(target, "original bytes");
        assertThrows(IOException.class, () -> storage.saveAddressBook(getTypicalAddressBook()));
        assertEquals("original bytes", Files.readString(target));
        assertOnlyTargetRemains();
    }

    @Test
    public void save_unsupportedAtomicMove_preservesExistingOrAbsentTarget() throws Exception {
        Path target = folder.resolve("roster.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(target) {
            @Override
            protected void replaceFile(Path temporary, Path destination) throws IOException {
                throw new AtomicMoveNotSupportedException(temporary.toString(), destination.toString(), "injected");
            }
        };
        assertThrows(IOException.class, () -> storage.saveAddressBook(getTypicalAddressBook()));
        assertFalse(Files.exists(target));
        try (var files = Files.list(folder)) {
            assertEquals(0, files.count());
        }
        Files.writeString(target, "original bytes");
        assertThrows(IOException.class, () -> storage.saveAddressBook(getTypicalAddressBook()));
        assertEquals("original bytes", Files.readString(target));
        assertOnlyTargetRemains();
    }

    @Test
    public void read_invalidOrDuplicateRecords_rejectsWholeFileWithoutWriting() throws Exception {
        Path target = folder.resolve("roster.json");
        String valid = JsonUtil.toJsonString(new JsonSerializableAddressBook(getTypicalAddressBook()));
        String person = JsonUtil.toJsonString(new JsonAdaptedPerson(getTypicalAddressBook().getPersonList().get(0)));
        for (String invalid : new String[] {"null", "{\"persons\":[null]}", valid + " trailing garbage", valid + valid,
            valid.replace("Alice Pauline", "Invalid/Name"), "{\"persons\":[" + person + "," + person + "]}"}) {
            Files.writeString(target, invalid);
            byte[] original = Files.readAllBytes(target);
            assertThrows(DataLoadingException.class, () -> new JsonAddressBookStorage(target).readAddressBook());
            assertArrayEquals(original, Files.readAllBytes(target));
        }
    }

    @Test
    public void save_nestedPathAndValidEmptyRoster_roundTrips() throws Exception {
        Path target = folder.resolve("nested/roster.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(target);
        storage.saveAddressBook(new AddressBook());
        assertEquals(new AddressBook(), storage.readAddressBook().orElseThrow());
        storage.saveAddressBook(getTypicalAddressBook());
        assertEquals(getTypicalAddressBook(), storage.readAddressBook().orElseThrow());
    }

    private void assertOnlyTargetRemains() throws IOException {
        try (var files = Files.list(folder)) {
            assertEquals(1, files.count());
        }
    }
}
