package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.commands.exceptions.DuplicateStudentException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Exercises complete command parsing, model, persistence, selection requests and failure recovery together. */
public class StudentWorkflowTest {
    @TempDir
    public Path folder;
    private ModelManager model;
    private CountingStorage storage;
    private LogicManager logic;
    private Person alex;
    private Person mei;
    private byte[] originalBytes;

    @BeforeEach
    public void setUp() throws Exception {
        alex = new PersonBuilder().withName("Alex Tan").withEmail("alex@u.nus.edu").withSample(true)
                .withTelegram("Alex_Tan").withGitHub("AlexTan").withRemark("Retained").withTags("demo")
                .withEnrolments(new Enrolment(new ModuleCode("CS2103T"), new Semester("AY26/27 S1"))).build();
        mei = new PersonBuilder().withName("Mei Tan").withEmail("mei@u.nus.edu").build();
        model = new ModelManager();
        model.addPerson(mei);
        model.addPerson(alex);
        storage = new CountingStorage(folder.resolve("roster.json"));
        new JsonAddressBookStorage(storage.getAddressBookFilePath()).saveAddressBook(model.getAddressBook());
        originalBytes = Files.readAllBytes(storage.getAddressBookFilePath());
        logic = new LogicManager(model, new StorageManager(storage, new JsonUserPrefsStorage(folder.resolve("prefs"))));
    }

    @Test
    public void execute_hiddenDuplicate_revealsTargetWithoutSavingOrOverwriting() throws Exception {
        logic.execute("find Mei");
        AtomicReference<Person> selected = new AtomicReference<>();
        assertThrows(DuplicateStudentException.class, () -> logic.execute(
                "student add /name Different /email ALEX@U.NUS.EDU /github other",
                result -> selected.set(result.getSelectionTarget())));
        assertEquals(alex, selected.get());
        assertEquals(List.of(alex, mei), model.getFilteredPersonList());
        assertUnchangedFile(0);
    }

    @Test
    public void execute_noOp_selectsEvenWhenRowsUnchangedAndDoesNotSave() throws Exception {
        logic.execute("list");
        AtomicReference<Person> selected = new AtomicReference<>();
        assertEquals("No values changed.", logic.execute("edit /email ALEX@U.NUS.EDU /telegram @Alex_Tan",
                result -> selected.set(result.getSelectionTarget())).getFeedbackToUser());
        assertEquals(alex, selected.get());
        assertEquals(List.of(alex, mei), model.getFilteredPersonList());
        selected.set(null);
        logic.execute("edit /email mei@u.nus.edu /github clear", result -> selected.set(result.getSelectionTarget()));
        assertEquals(mei, selected.get());
        assertUnchangedFile(0);
    }

    @Test
    public void execute_caseOnlyAndClear_persistAndPreserveTeachingContext() throws Exception {
        logic.execute("find Mei");
        assertEquals("Updated student: Alex Tan.", logic.execute(
                "edit /email alex@u.nus.edu /telegram alex_tan /github clear").getFeedbackToUser());
        Person updated = model.getFilteredPersonList().get(0);
        assertEquals("alex_tan", updated.getTelegram().orElseThrow().value);
        assertTrue(updated.getGitHub().isEmpty());
        assertEquals(alex.getName(), updated.getName());
        assertEquals(alex.getEmail(), updated.getEmail());
        assertEquals(alex.getEnrolments(), updated.getEnrolments());
        assertEquals(alex.getTags(), updated.getTags());
        assertEquals(alex.getRemark(), updated.getRemark());
        assertTrue(updated.isSample());
        assertEquals(1, storage.attempts);
        assertEquals(model.getAddressBook(), new AddressBook(storage.readAddressBook().orElseThrow()));
        logic.execute("edit /email alex@u.nus.edu /telegram @clear /github Clear");
        Person literal = model.getFilteredPersonList().get(0);
        assertEquals("clear", literal.getTelegram().orElseThrow().value);
        assertEquals("Clear", literal.getGitHub().orElseThrow().value);
        assertEquals(2, storage.attempts);
    }

    @Test
    public void execute_failedSave_restoresFilterAndAllowsSafeRetry() throws Exception {
        logic.execute("find Mei");
        storage.fail = true;
        assertEquals("The record could not be updated. No data was changed.", assertThrows(CommandException.class, () ->
                logic.execute("edit /email alex@u.nus.edu /github changed")).getMessage());
        assertEquals(List.of(mei), model.getFilteredPersonList());
        assertEquals(List.of(mei, alex), model.getAddressBook().getPersonList());
        assertUnchangedFile(1);
        assertEquals("The student could not be saved. No data was changed.", assertThrows(CommandException.class, () ->
                logic.execute("student add /name New /email new@u.nus.edu")).getMessage());
        assertEquals(List.of(mei), model.getFilteredPersonList());
        assertUnchangedFile(2);
        storage.fail = false;
        logic.execute("student add /name New /email new@u.nus.edu");
        assertEquals(3, storage.attempts);
        assertEquals(3, storage.readAddressBook().orElseThrow().getPersonList().size());
    }

    @Test
    public void execute_failedPresentation_rollsBackChangesNoOpAndDuplicateReveal() throws Exception {
        for (String command : new String[] {"student add /name New /email new@u.nus.edu",
            "edit /email alex@u.nus.edu /github changed", "edit /email alex@u.nus.edu /telegram @Alex_Tan",
            "student add /name Duplicate /email ALEX@U.NUS.EDU"}) {
            logic.execute("find Mei");
            assertEquals(Messages.MESSAGE_PROFILE_DISPLAY_FAILURE, assertThrows(CommandException.class, () ->
                    logic.execute(command, result -> {
                        throw new AssertionError("Injected display failure");
                    }))
                    .getMessage());
            assertEquals(List.of(mei), model.getFilteredPersonList());
            assertEquals(List.of(mei, alex), model.getAddressBook().getPersonList());
            assertUnchangedFile(0);
        }
    }

    private void assertUnchangedFile(int attempts) throws IOException {
        assertEquals(attempts, storage.attempts);
        assertArrayEquals(originalBytes, Files.readAllBytes(storage.getAddressBookFilePath()));
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
                throw new IOException("Injected failure");
            }
            super.saveAddressBook(roster);
        }
    }
}
