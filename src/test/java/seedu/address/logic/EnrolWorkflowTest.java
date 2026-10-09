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

import seedu.address.logic.commands.EnrolCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Full parser/model/storage coverage of enrolment and protected failure paths. */
public class EnrolWorkflowTest {
    private static final String COMMAND = "enrol /email E9000001@U.NUS.EDU /module cs2103t /semester ay26/27 s1";
    @TempDir
    public Path folder;
    private Person first;
    private Person second;
    private ModelManager model;
    private ProbeStorage storage;
    private LogicManager logic;
    private byte[] originalBytes;

    @BeforeEach
    public void setUp() throws Exception {
        first = new PersonBuilder().withName("Alex Tan").withEmail("e9000001@u.nus.edu").withSample(true)
                .withTelegram("Alex_Tan").withGitHub("alex-tan").withRemark("retained").withTags("demo")
                .withEnrolments(new Enrolment(new ModuleCode("MA1521"), new Semester("AY25/26 S2"))).build();
        second = new PersonBuilder(first).withEmail("e9000002@u.nus.edu").withSample(false).build();
        model = new ModelManager();
        model.addPerson(second);
        model.addPerson(first);
        storage = new ProbeStorage(folder.resolve("roster.json"));
        new JsonAddressBookStorage(storage.getAddressBookFilePath()).saveAddressBook(model.getAddressBook());
        originalBytes = Files.readAllBytes(storage.getAddressBookFilePath());
        logic = newLogic(model);
    }

    @Test
    public void execute_hiddenSameNameOwner_persistsAndPreservesCompleteRecord() throws Exception {
        logic.execute("find e9000002");
        AtomicReference<Person> selected = new AtomicReference<>();
        String feedback = logic.execute(COMMAND + " /section Lab\t 2 /team SEED", result -> {
            selected.set(result.getSelectionTarget());
            // Existing callback prepares display before saving; feedback is returned only after save.
            assertEquals(0, storage.attempts);
        }).getFeedbackToUser();
        assertEquals("Added enrolment for Alex Tan: CS2103T, AY26/27 S1.\nModule: CS2103T\n"
                + "Semester: AY26/27 S1\nSection: Lab 2\nTeam: SEED", feedback);
        Person updated = selected.get();
        assertEquals(2, updated.getEnrolments().size());
        assertEquals(first.getEnrolments().getFirst(), updated.getEnrolments().getFirst());
        assertEquals(first, updated.withEnrolments(first.getEnrolments()));
        assertTrue(updated.isSample());
        assertEquals(List.of(updated, second), model.getFilteredPersonList());
        assertEquals(1, storage.attempts);
        assertEquals(model.getAddressBook(), new AddressBook(storage.readAddressBook().orElseThrow()));
        ModelManager restarted = new ModelManager(storage.readAddressBook().orElseThrow(), new UserPrefs());
        assertEquals(updated, restarted.getAddressBook().getPersonList().get(1));
    }

    @Test
    public void execute_absentAffiliationsAndDifferentContextsAndOwners() throws Exception {
        String result = logic.execute(COMMAND).getFeedbackToUser();
        assertTrue(result.endsWith("Section: Not assigned\nTeam: Not assigned"));
        logic.execute(COMMAND.replace("s1", "s2"));
        logic.execute(COMMAND.replace("cs2103t", "cs2113t"));
        logic.execute(COMMAND.replace("E9000001", "E9000002"));
        assertEquals(4, model.getFilteredPersonList().getFirst().getEnrolments().size());
        assertEquals(2, model.getFilteredPersonList().get(1).getEnrolments().size());
        assertEquals(model.getAddressBook(), new AddressBook(storage.readAddressBook().orElseThrow()));
    }

    @Test
    public void execute_duplicateIgnoresAffiliations_preservesFilterAndBytes() throws Exception {
        logic.execute(COMMAND + " /section T12 /team SEED");
        byte[] saved = Files.readAllBytes(storage.getAddressBookFilePath());
        logic.execute("find e9000002");
        ReadOnlyAddressBook previous = new AddressBook(model.getAddressBook());
        assertEquals(String.format(EnrolCommand.MESSAGE_DUPLICATE, "CS2103T", "AY26/27 S1"),
                assertThrows(CommandException.class, () -> logic.execute(COMMAND.replace("s1", "  S1")
                        + " /team Changed")).getMessage());
        assertEquals(List.of(second), model.getFilteredPersonList());
        assertEquals(previous, model.getAddressBook());
        assertEquals(1, storage.attempts);
        assertArrayEquals(saved, Files.readAllBytes(storage.getAddressBookFilePath()));
    }

    @Test
    public void execute_invalidAndMissingOwner_preserveDataAndDisplay() throws Exception {
        logic.execute("find e9000002");
        assertEquals(String.format(EnrolCommand.MESSAGE_MISSING_STUDENT, "unknown@u.nus.edu"),
                assertThrows(CommandException.class, () -> logic.execute(
                        COMMAND.replace("E9000001@U.NUS.EDU", "unknown@u.nus.edu"))).getMessage());
        assertThrows(ParseException.class, () -> logic.execute(COMMAND + " /team _"));
        assertEquals(List.of(second), model.getFilteredPersonList());
        assertUnchanged(0);
    }

    @Test
    public void execute_failedSave_restoresFilterComparatorAndAllowsRetry() throws Exception {
        logic.execute("find e9000002");
        storage.fail = true;
        assertEquals(EnrolCommand.MESSAGE_SAVE_FAILURE,
                assertThrows(CommandException.class, () -> logic.execute(COMMAND)).getMessage());
        assertEquals(List.of(second), model.getFilteredPersonList());
        assertEquals(List.of(second, first), model.getAddressBook().getPersonList());
        assertUnchanged(1);
        storage.fail = false;
        logic.execute(COMMAND);
        assertEquals(2, storage.attempts);
        assertEquals(2, model.getFilteredPersonList().getFirst().getEnrolments().size());
        assertEquals(model.getAddressBook(), new AddressBook(storage.readAddressBook().orElseThrow()));
    }

    @Test
    public void execute_failedPresentation_doesNotSave() throws Exception {
        logic.execute("find e9000002");
        assertEquals(Messages.MESSAGE_PROFILE_DISPLAY_FAILURE,
                assertThrows(CommandException.class, () -> logic.execute(COMMAND, result -> {
                    throw new AssertionError("Injected rendering failure");
                })).getMessage());
        assertEquals(List.of(second), model.getFilteredPersonList());
        assertUnchanged(0);
    }

    @Test
    public void execute_recoveryMode_rejectsEnrolmentWithoutSaving() throws Exception {
        ModelManager recovery = new ModelManager(model.getAddressBook(), new UserPrefs(), true);
        assertEquals(LogicManager.MESSAGE_READ_ONLY,
                assertThrows(CommandException.class, () -> newLogic(recovery).execute(COMMAND)).getMessage());
        assertEquals(List.of(second, first), recovery.getAddressBook().getPersonList());
        assertUnchanged(0);
    }

    private LogicManager newLogic(ModelManager target) {
        return new LogicManager(target, new StorageManager(storage,
                new JsonUserPrefsStorage(folder.resolve("prefs.json"))));
    }

    private void assertUnchanged(int attempts) throws IOException {
        assertEquals(attempts, storage.attempts);
        assertArrayEquals(originalBytes, Files.readAllBytes(storage.getAddressBookFilePath()));
        assertEquals(List.of(second, first), model.getAddressBook().getPersonList());
    }

    private static class ProbeStorage extends JsonAddressBookStorage {
        private int attempts;
        private boolean fail;

        ProbeStorage(Path path) {
            super(path);
        }

        @Override
        public void saveAddressBook(ReadOnlyAddressBook data) throws IOException {
            attempts++;
            if (fail) {
                throw new IOException("Injected save failure");
            }
            super.saveAddressBook(data);
        }
    }
}
