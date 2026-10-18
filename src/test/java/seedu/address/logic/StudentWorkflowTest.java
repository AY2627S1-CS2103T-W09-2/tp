package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.ConfirmDeleteCommand;
import seedu.address.logic.commands.EditCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.commands.exceptions.DuplicateStudentException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.person.GitHub;
import seedu.address.model.person.Person;
import seedu.address.model.person.Telegram;
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
            "edit /email alex@u.nus.edu /new-email new@u.nus.edu /name New Name",
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

    @Test
    public void execute_identityChange_revealsUpdatedStudentPersistsAndRetiresOldEmail() throws Exception {
        logic.execute("find Mei");
        AtomicReference<Person> selected = new AtomicReference<>();
        assertEquals("Updated student: Zed Lim.", logic.execute(
                "edit /email alex@u.nus.edu /new-email ZED@U.NUS.EDU /name Zed \t Lim /github clear",
                result -> selected.set(result.getSelectionTarget())).getFeedbackToUser());
        Person expected = new PersonBuilder(alex).withName("Zed Lim").withEmail("zed@u.nus.edu").withoutContacts()
                .withTelegram("Alex_Tan").build();
        assertEquals(expected, selected.get());
        assertEquals(List.of(mei, expected), model.getFilteredPersonList());
        assertEquals(1, storage.attempts);
        for (String oldEmail : new String[] {"view alex@u.nus.edu", "edit /email alex@u.nus.edu /github AlexTan"}) {
            assertEquals("No student found with NUS email: alex@u.nus.edu.",
                    assertThrows(CommandException.class, () -> logic.execute(oldEmail)).getMessage());
        }
        assertEquals(1, storage.attempts);

        ModelManager restarted = new ModelManager(storage.readAddressBook().orElseThrow(), new UserPrefs());
        assertEquals(List.of(mei, expected), restarted.getAddressBook().getPersonList());
        LogicManager restartedLogic = new LogicManager(restarted,
                new StorageManager(storage, new JsonUserPrefsStorage(folder.resolve("prefs"))));
        assertEquals("Viewing student: Zed Lim.", restartedLogic.execute("view zed@u.nus.edu").getFeedbackToUser());
        assertThrows(CommandException.class, () -> restartedLogic.execute("view alex@u.nus.edu"));
    }

    @Test
    public void execute_sameNameAsAnotherStudent_allowedAndSelectsEditedProfile() throws Exception {
        AtomicReference<Person> selected = new AtomicReference<>();
        logic.execute("edit /email alex@u.nus.edu /name Mei Tan", result -> selected.set(result.getSelectionTarget()));
        Person renamed = new PersonBuilder(alex).withName("Mei Tan").build();
        assertEquals(List.of(renamed, mei), model.getFilteredPersonList()); // equal names are ordered by email
        assertEquals(renamed, selected.get());
        logic.execute("edit /email alex@u.nus.edu /new-email zed@u.nus.edu",
                result -> selected.set(result.getSelectionTarget()));
        Person moved = new PersonBuilder(renamed).withEmail("zed@u.nus.edu").build();
        assertEquals(List.of(mei, moved), model.getFilteredPersonList());
        assertEquals(moved, selected.get());
        assertEquals(2, storage.attempts);
    }

    @Test
    public void execute_mixedValidAndInvalidIdentityEdits_changeNothing() throws Exception {
        logic.execute("find Mei");
        AtomicBoolean presented = new AtomicBoolean();
        String edit = "edit /email alex@u.nus.edu ";
        String[][] cases = {
            {edit + "/name Unused Name /new-email Mei@U.NUS.EDU", EditCommand.MESSAGE_DUPLICATE_EMAIL},
            {edit + "/new-email new@u.nus.edu /github bad_", GitHub.MESSAGE_CONSTRAINTS},
            {edit + "/name New Name /telegram four", Telegram.MESSAGE_CONSTRAINTS}};
        for (String[] rejected : cases) {
            Exception error = assertThrows(Exception.class, () -> logic.execute(rejected[0],
                    result -> presented.set(true)));
            assertEquals(rejected[1], error.getMessage());
        }
        assertFalse(presented.get());
        assertEquals(List.of(mei), model.getFilteredPersonList());
        assertEquals(List.of(mei, alex), model.getAddressBook().getPersonList());
        assertUnchangedFile(0);
    }

    @Test
    public void execute_normalisedIdentityNoOps_revealWithoutSavingButCaseOnlyNameSaves() throws Exception {
        for (String command : new String[] {"edit /email alex@u.nus.edu /name \tAlex   Tan ",
            "edit /email alex@u.nus.edu /new-email ALEX@U.NUS.EDU",
            "edit /email mei@u.nus.edu /new-email Mei@u.nus.edu /telegram clear"}) {
            logic.execute("find nothing-matches");
            AtomicReference<Person> selected = new AtomicReference<>();
            assertEquals("No values changed.", logic.execute(command,
                    result -> selected.set(result.getSelectionTarget())).getFeedbackToUser());
            assertEquals(command.contains("mei@") ? mei : alex, selected.get());
            assertEquals(List.of(alex, mei), model.getFilteredPersonList());
            assertUnchangedFile(0);
        }
        assertEquals("Updated student: alex TAN.", logic.execute("edit /email alex@u.nus.edu /name alex TAN")
                .getFeedbackToUser());
        assertEquals(List.of(mei, new PersonBuilder(alex).withName("alex TAN").build()),
                model.getAddressBook().getPersonList());
        assertEquals(1, storage.attempts);
    }

    @Test
    public void execute_failedIdentitySave_keepsOldEmailAndAllowsRetry() throws Exception {
        String command = "edit /email alex@u.nus.edu /new-email new@u.nus.edu /name New Name";
        logic.execute("find Mei");
        storage.fail = true;
        assertEquals("The record could not be updated. No data was changed.",
                assertThrows(CommandException.class, () -> logic.execute(command)).getMessage());
        assertEquals(List.of(mei), model.getFilteredPersonList());
        assertEquals(List.of(mei, alex), model.getAddressBook().getPersonList());
        assertUnchangedFile(1);
        storage.fail = false;
        logic.execute(command);
        Person updated = new PersonBuilder(alex).withName("New Name").withEmail("new@u.nus.edu").build();
        assertEquals(List.of(mei, updated), model.getAddressBook().getPersonList());
        assertEquals(List.of(mei, updated), storage.readAddressBook().orElseThrow().getPersonList());
        assertEquals(2, storage.attempts);
    }

    @Test
    public void execute_identityEdit_cancelsPendingDeletion() throws Exception {
        logic.execute("delete alex@u.nus.edu");
        assertTrue(logic.hasPendingDeletion());
        assertEquals(Messages.MESSAGE_PENDING_DELETION_CANCELLED + "\nUpdated student: Alex Tan.",
                logic.execute("edit /email alex@u.nus.edu /new-email new@u.nus.edu").getFeedbackToUser());
        assertFalse(logic.hasPendingDeletion());
        assertEquals(ConfirmDeleteCommand.MESSAGE_NO_PENDING, assertThrows(CommandException.class, () ->
                logic.execute("confirm-delete new@u.nus.edu")).getMessage());
        assertEquals(2, model.getAddressBook().getPersonList().size());
        assertEquals(1, storage.attempts);
    }

    @Test
    public void execute_recoveryRejectsIdentityChangesAndNoOps() throws Exception {
        LogicManager recovery = new LogicManager(new ModelManager(model.getAddressBook(), new UserPrefs(), true),
                new StorageManager(storage, new JsonUserPrefsStorage(folder.resolve("prefs"))));
        for (String command : new String[] {"edit /email alex@u.nus.edu /new-email new@u.nus.edu /name New",
            "edit /email alex@u.nus.edu /name Alex Tan"}) {
            assertEquals(LogicManager.MESSAGE_READ_ONLY,
                    assertThrows(CommandException.class, () -> recovery.execute(command)).getMessage());
        }
        assertUnchangedFile(0);
    }

    @Test
    public void execute_view_selectsCanonicalTargetWithoutSaving() throws Exception {
        logic.execute("find Mei");
        AtomicReference<Person> selected = new AtomicReference<>();
        assertEquals("Viewing student: Alex Tan.", logic.execute("view \tALEX@U.NUS.EDU ",
                result -> selected.set(result.getSelectionTarget())).getFeedbackToUser());
        assertEquals(alex, selected.get());
        assertEquals(List.of(alex, mei), model.getFilteredPersonList());
        assertUnchangedFile(0);
    }

    @Test
    public void execute_viewDisplayFailure_restoresResultsWithRetryGuidance() throws Exception {
        logic.execute("find Mei");
        assertEquals("Profile could not be displayed. Try viewing the student again.",
                assertThrows(CommandException.class, () -> logic.execute("view alex@u.nus.edu", result -> {
                    throw new AssertionError("Injected display failure");
                })).getMessage());
        assertEquals(List.of(mei), model.getFilteredPersonList());
        assertUnchangedFile(0);
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
