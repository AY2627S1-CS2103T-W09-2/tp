package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

public class MainAppTest {
    @TempDir
    public Path folder;

    @Test
    public void initModel_missingFile_startsEmptyWithoutWriting() {
        MainApp app = createApp();
        assertFalse(app.model.isReadOnly());
        assertTrue(app.model.getAddressBook().getPersonList().isEmpty());
        assertFalse(Files.exists(app.storage.getAddressBookFilePath()));
    }

    @Test
    public void initModel_validEmptyAndPopulatedFiles_restoresData() throws Exception {
        for (AddressBook roster : new AddressBook[] {new AddressBook(), getTypicalAddressBook()}) {
            new JsonAddressBookStorage(folder.resolve("roster.json")).saveAddressBook(roster);
            MainApp app = createApp();
            assertFalse(app.model.isReadOnly());
            assertEquals(roster, app.model.getAddressBook());
        }
    }

    @Test
    public void initModel_invalidFile_entersRecoveryAndPreservesFileThroughExit() throws Exception {
        for (String invalid : new String[] {"broken JSON", "null", "{}", "{\"persons\":[null]}",
            "{\"persons\":[]} broken JSON", "{\"persons\":[]} {\"persons\":[{\"name\":\"lost record\"}]}"}) {
            Path path = folder.resolve("roster.json");
            Files.writeString(path, invalid);
            byte[] original = Files.readAllBytes(path);
            MainApp app = createApp();
            assertTrue(app.model.isReadOnly());
            assertTrue(app.model.getAddressBook().getPersonList().isEmpty());
            app.logic.execute("list");
            app.logic.execute("find Nobody");
            app.logic.execute("help");
            assertThrows(CommandException.class, LogicManager.MESSAGE_READ_ONLY, () -> app.logic.execute("clear"));
            String add = "add n/Fictional Student p/12345 e/demo@u.nus.edu a/Test";
            assertThrows(CommandException.class, LogicManager.MESSAGE_READ_ONLY, () -> app.logic.execute(add));
            app.logic.execute("exit");
            app.stop();
            assertArrayEquals(original, Files.readAllBytes(path));
        }
    }

    @Test
    public void initModel_unreadablePath_entersRecovery() throws Exception {
        Files.createDirectory(folder.resolve("roster.json"));
        assertTrue(createApp().model.isReadOnly());
    }

    @Test
    public void initModel_invalidUtf8_preservesBytesInRecovery() throws Exception {
        Path path = folder.resolve("roster.json");
        new JsonAddressBookStorage(path).saveAddressBook(getTypicalAddressBook());
        byte[] bytes = Files.readAllBytes(path);
        String json = Files.readString(path);
        int addressStart = json.indexOf("123, Jurong West Ave 6, #08-111");
        assertTrue(addressStart >= 0);
        bytes[addressStart] = (byte) 0x80;
        Files.write(path, bytes);
        MainApp app = createApp();
        assertTrue(app.model.isReadOnly());
        app.logic.execute("list");
        app.stop();
        assertArrayEquals(bytes, Files.readAllBytes(path));
    }

    @Test
    public void initModel_incompatibleEmailFile_entersRecoveryAndPreservesFileThroughExit() throws Exception {
        Path path = folder.resolve("roster.json");
        new JsonAddressBookStorage(path).saveAddressBook(getTypicalAddressBook());
        assertEquals(getTypicalAddressBook(), new JsonAddressBookStorage(path).readAddressBook().orElseThrow());
        String validJson = Files.readString(path);

        String legacy = Files.readString(
                Paths.get("src", "test", "data", "JsonAddressBookStorageTest", "legacyAb3EmailAddressBook.json"));
        String nonCanonical = replaceOnce(validJson, "\"alice@u.nus.edu\"", "\"ALICE@u.nus.edu\"");
        String duplicate = replaceOnce(validJson, "\"johnd@u.nus.edu\"", "\"alice@u.nus.edu\"");
        for (String incompatible : new String[] {legacy, nonCanonical, duplicate}) {
            assertRecoveryPreservesFileThroughExit(path, incompatible);
        }
    }

    @Test
    public void initModel_missingOrInvalidSampleClassification_entersRecoveryAndPreservesFile() throws Exception {
        Path path = folder.resolve("roster.json");
        new JsonAddressBookStorage(path).saveAddressBook(getTypicalAddressBook());
        assertEquals(getTypicalAddressBook(), new JsonAddressBookStorage(path).readAddressBook().orElseThrow());
        ObjectMapper mapper = new ObjectMapper();
        String validJson = Files.readString(path);

        // an older file without the classification, and classifications that are null or not booleans
        JsonNode missing = mapper.readTree(validJson);
        secondRecord(missing).remove("sample");
        JsonNode nullSample = mapper.readTree(validJson);
        secondRecord(nullSample).putNull("sample");
        JsonNode textSample = mapper.readTree(validJson);
        secondRecord(textSample).put("sample", "false");
        for (JsonNode incompatible : new JsonNode[] {missing, nullSample, textSample}) {
            assertRecoveryPreservesFileThroughExit(path, mapper.writeValueAsString(incompatible));
        }
    }

    private static ObjectNode secondRecord(JsonNode roster) {
        return (ObjectNode) roster.get("persons").get(1);
    }

    /**
     * Writes {@code incompatible} to {@code path}, starts the app on it, and asserts that the app opens an empty
     * read-only recovery session that blocks mutations and leaves the file's bytes unchanged through exit.
     */
    private void assertRecoveryPreservesFileThroughExit(Path path, String incompatible) throws Exception {
        Files.writeString(path, incompatible);
        byte[] original = Files.readAllBytes(path);
        MainApp app = createApp();
        assertTrue(app.model.isReadOnly());
        assertTrue(app.model.getAddressBook().getPersonList().isEmpty());
        app.logic.execute("list");
        app.logic.execute("find alice");
        for (String mutation : new String[] {"add n/Fictional Student p/12345 e/demo@u.nus.edu a/Test",
            "edit 1 e/demo@u.nus.edu"}) {
            assertThrows(CommandException.class, LogicManager.MESSAGE_READ_ONLY, () -> app.logic.execute(mutation));
            assertArrayEquals(original, Files.readAllBytes(path));
        }
        app.logic.execute("exit");
        app.stop();
        assertArrayEquals(original, Files.readAllBytes(path));
    }

    /**
     * Replaces the only occurrence of {@code target} in {@code text}.
     */
    private static String replaceOnce(String text, String target, String replacement) {
        assertEquals(text.length() - target.length(), text.replace(target, "").length());
        return text.replace(target, replacement);
    }

    private MainApp createApp() {
        MainApp app = new MainApp();
        app.storage = new StorageManager(new JsonAddressBookStorage(folder.resolve("roster.json")),
                new JsonUserPrefsStorage(folder.resolve("prefs.json")));
        Model model = app.initModelManager(app.storage, new UserPrefs());
        app.model = model;
        app.logic = new LogicManager(model, app.storage);
        return app;
    }
}
