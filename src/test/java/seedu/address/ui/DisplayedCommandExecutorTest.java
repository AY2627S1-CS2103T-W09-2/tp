package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

public class DisplayedCommandExecutorTest {
    @TempDir
    public Path temporaryFolder;

    private final Person alice = new PersonBuilder().withName("Alice").withEmail("alice@u.nus.edu").build();
    private final Person bob = new PersonBuilder().withName("Bob").withEmail("bob@u.nus.edu").build();
    private final Person carl = new PersonBuilder().withName("Carl").withEmail("carl@u.nus.edu").build();
    private ModelManager model;
    private FailingStorage storage;
    private DisplayedCommandExecutor executor;
    private List<Person> displayed;
    private byte[] savedBytes;
    private boolean isDisplayFailing;

    @BeforeEach
    public void setUp() throws Exception {
        model = new ModelManager();
        model.addPerson(alice);
        model.addPerson(bob);
        model.addPerson(carl);
        storage = new FailingStorage(temporaryFolder.resolve("roster.json"));
        storage.saveAddressBook(model.getAddressBook());
        savedBytes = Files.readAllBytes(storage.getAddressBookFilePath());
        displayed = List.copyOf(model.getFilteredPersonList());
        LogicManager logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));
        executor = new DisplayedCommandExecutor(logic, () -> displayed.equals(model.getFilteredPersonList()),
                result -> refreshDisplay(), this::refreshDisplay);
    }

    // `remark INDEX` is the remaining index-targeted command that saves and then refreshes the display.
    // Each attempt uses a distinct remark so that a repeated or misdirected mutation is observable.

    @Test
    public void execute_failedIndexCommandThenRetry_targetsDisplayedStudent() throws Exception {
        storage.isFailing = true;
        CommandException failure = assertThrows(CommandException.class, () ->
                executor.execute("remark 1 r/First"));
        assertTrue(failure.getMessage().contains("Injected save failure"));
        assertArrayEquals(savedBytes, Files.readAllBytes(storage.getAddressBookFilePath()));
        assertEquals(List.of(alice, bob, carl), displayed);
        assertEquals(model.getFilteredPersonList(), displayed);
        Person visibleTarget = displayed.get(1);

        storage.isFailing = false;
        executor.execute("remark 2 r/Target");
        assertEquals(List.of(alice, withRemark(visibleTarget, "Target"), carl), displayed);
        assertEquals(displayed, storage.readAddressBook().orElseThrow().getPersonList());
    }

    @Test
    public void execute_failedFilteredEditThenIndexCommand_preservesDisplayedTarget() throws Exception {
        executor.execute("find Bob");
        assertEquals(List.of(bob), displayed);
        storage.isFailing = true;
        assertThrows(CommandException.class, () -> executor.execute("edit /email bob@u.nus.edu /github Zoe"));
        assertEquals(List.of(bob), displayed);
        assertArrayEquals(savedBytes, Files.readAllBytes(storage.getAddressBookFilePath()));

        storage.isFailing = false;
        executor.execute("remark 1 r/Target");
        // Index 1 referred to the displayed Bob, not the first stored student; remark then shows everyone.
        assertEquals(List.of(alice, withRemark(bob, "Target"), carl), displayed);
        assertEquals(displayed, storage.readAddressBook().orElseThrow().getPersonList());
    }

    @Test
    public void execute_failedSaveWithUnavailableDisplay_retainsOriginalSnapshot() throws Exception {
        List<Person> previous = displayed;
        storage.isFailing = true;
        isDisplayFailing = true;
        CommandException failure = assertThrows(CommandException.class, () ->
                executor.execute("remark 1 r/First"));
        assertTrue(failure.getMessage().contains("Injected save failure"));
        assertSame(previous, displayed);
        assertEquals(displayed, model.getFilteredPersonList());
        assertArrayEquals(savedBytes, Files.readAllBytes(storage.getAddressBookFilePath()));

        assertThrows(CommandException.class, () -> executor.execute("remark 2 r/Second"));
        assertSame(previous, displayed);
        assertEquals(displayed, model.getFilteredPersonList());
        assertArrayEquals(savedBytes, Files.readAllBytes(storage.getAddressBookFilePath()));

        storage.isFailing = false;
        isDisplayFailing = false;
        executor.execute("remark 2 r/Third");
        assertEquals(List.of(alice, withRemark(bob, "Third"), carl), displayed);
        assertEquals(displayed, storage.readAddressBook().orElseThrow().getPersonList());
    }

    @Test
    public void execute_savedCommandWithDisplayFailure_doesNotRepeatMutationOnRefresh() throws Exception {
        isDisplayFailing = true;
        assertThrows(CommandException.class, () -> executor.execute("remark 1 r/First"));
        List<Person> savedOnce = List.of(withRemark(alice, "First"), bob, carl);
        assertEquals(savedOnce, storage.readAddressBook().orElseThrow().getPersonList());
        isDisplayFailing = false;
        assertThrows(CommandException.class, () -> executor.execute("remark 1 r/Second"));
        assertEquals(savedOnce, displayed);
        assertEquals(displayed, storage.readAddressBook().orElseThrow().getPersonList());
    }

    @Test
    public void execute_failedSearchOrInvalidInput_preservesPreviousSnapshot() throws Exception {
        executor.execute("find Bob");
        List<Person> previous = displayed;
        isDisplayFailing = true;
        CommandException failure = assertThrows(CommandException.class, () -> executor.execute("find Alice"));
        assertEquals(Messages.MESSAGE_SEARCH_DISPLAY_FAILURE, failure.getMessage());
        assertSame(previous, displayed);
        assertEquals(displayed, model.getFilteredPersonList());
        assertThrows(ParseException.class, () -> executor.execute("find"));
        assertSame(previous, displayed);
        assertArrayEquals(savedBytes, Files.readAllBytes(storage.getAddressBookFilePath()));
    }

    private void refreshDisplay() {
        if (isDisplayFailing) {
            throw new AssertionError("Injected FXML load failure");
        }
        displayed = List.copyOf(model.getFilteredPersonList());
    }

    private static Person withRemark(Person person, String remark) {
        return new PersonBuilder(person).withRemark(remark).build();
    }

    private static class FailingStorage extends JsonAddressBookStorage {
        private boolean isFailing;

        FailingStorage(Path path) {
            super(path);
        }

        @Override
        public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
            if (isFailing) {
                throw new IOException("Injected save failure");
            }
            super.saveAddressBook(addressBook);
        }
    }
}
