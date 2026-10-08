package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.SampleCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Exercises sample parsing, atomic persistence, restart and protected recovery together. */
public class SampleWorkflowTest {
    @TempDir
    public Path folder;

    private ModelManager model;
    private CountingStorage storage;
    private LogicManager logic;

    @BeforeEach
    public void setUp() {
        model = new ModelManager();
        storage = new CountingStorage(folder.resolve("roster.json"));
        logic = new LogicManager(model,
                new StorageManager(storage, new JsonUserPrefsStorage(folder.resolve("preferences.json"))));
    }

    @Test
    public void execute_load_persistsCompleteFixtureAndRestoresItAfterRestart() throws Exception {
        AtomicReference<CommandResult> presented = new AtomicReference<>();

        CommandResult result = logic.execute("sample   load", presented::set);

        assertEquals(SampleCommand.MESSAGE_SUCCESS, result.getFeedbackToUser());
        assertEquals(result, presented.get());
        assertNull(result.getSelectionTarget());
        assertEquals(1, storage.attempts);
        assertEquals(5, model.getAddressBook().getPersonList().size());
        assertEquals(5, model.getAddressBook().getPersonList().stream()
                .mapToInt(person -> person.getEnrolments().size()).sum());
        assertTrue(model.getAddressBook().getPersonList().stream().allMatch(Person::isSample));
        assertEquals(List.of("e9000001@u.nus.edu", "e9000002@u.nus.edu", "e9000003@u.nus.edu",
                "e9000005@u.nus.edu", "e9000004@u.nus.edu"),
                model.getFilteredPersonList().stream().map(person -> person.getEmail().value).toList());

        ReadOnlyAddressBook reloaded = storage.readAddressBook().orElseThrow();
        assertEquals(model.getAddressBook(), new AddressBook(reloaded));
        assertTrue(reloaded.getPersonList().stream().allMatch(Person::isSample));
    }

    @Test
    public void execute_repeatedLoad_rejectsWithoutWritingOrChangingFile() throws Exception {
        logic.execute("sample load");
        byte[] savedBytes = Files.readAllBytes(storage.getAddressBookFilePath());

        CommandException error = assertThrows(CommandException.class, () -> logic.execute("sample load"));

        assertEquals(SampleCommand.MESSAGE_NON_EMPTY, error.getMessage());
        assertEquals(1, storage.attempts);
        assertArrayEquals(savedBytes, Files.readAllBytes(storage.getAddressBookFilePath()));
        assertEquals(5, model.getAddressBook().getPersonList().size());
    }

    @Test
    public void execute_saveFailure_restoresEmptyModelAndLeavesNoPartialFile() {
        storage.fail = true;

        CommandException error = assertThrows(CommandException.class, () -> logic.execute("sample load"));

        assertEquals(SampleCommand.MESSAGE_SAVE_FAILURE, error.getMessage());
        assertEquals(1, storage.attempts);
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertFalse(Files.exists(storage.getAddressBookFilePath()));
    }

    @Test
    public void execute_readOnlyRecovery_rejectsWithoutPreparingOrSavingSamples() {
        ModelManager recoveryModel = new ModelManager(new AddressBook(), new UserPrefs(), true);
        LogicManager recoveryLogic = new LogicManager(recoveryModel,
                new StorageManager(storage, new JsonUserPrefsStorage(folder.resolve("recovery-prefs.json"))));

        CommandException error = assertThrows(CommandException.class, () -> recoveryLogic.execute("sample load"));

        assertEquals(LogicManager.MESSAGE_READ_ONLY, error.getMessage());
        assertEquals(0, storage.attempts);
        assertTrue(recoveryModel.getAddressBook().getPersonList().isEmpty());
        assertFalse(Files.exists(storage.getAddressBookFilePath()));
    }

    private static class CountingStorage extends JsonAddressBookStorage {
        private int attempts;
        private boolean fail;

        CountingStorage(Path path) {
            super(path);
        }

        @Override
        public void saveAddressBook(ReadOnlyAddressBook roster) throws IOException {
            attempts++;
            if (fail) {
                throw new IOException("Injected sample save failure");
            }
            super.saveAddressBook(roster);
        }
    }
}
