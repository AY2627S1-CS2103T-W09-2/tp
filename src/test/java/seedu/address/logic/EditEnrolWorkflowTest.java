package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.EditEnrolCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Section;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.enrolment.Team;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Checks exact owner/context updates through real logic, JSON persistence and transactional failures. */
public class EditEnrolWorkflowTest {
    private static final String EDIT = "edit-enrol /email e9000001@u.nus.edu /module CS2103T /semester AY26/27 S1";
    private static final Enrolment FIRST = context("AY26/27 S1", "Lab 2", "SEED-2");
    private static final Enrolment SECOND = context("AY26/27 S2", "T1", "SEED-1");
    @TempDir
    public Path folder;

    private final Person owner = new PersonBuilder().withName("Alex Demo").withEmail("e9000001@u.nus.edu")
            .withTelegram("Alex_Demo").withGitHub("alex-demo").withRemark("Preserve").withTags("tutor")
            .withSample(true).withEnrolments(FIRST, SECOND).build();
    private final Person other = new PersonBuilder(owner).withEmail("e9000002@u.nus.edu").build();

    private ModelManager model;
    private CountingStorage storage;
    private LogicManager logic;
    private byte[] saved;

    @BeforeEach
    public void setUp() throws Exception {
        AddressBook roster = new AddressBook();
        roster.addPerson(other);
        roster.addPerson(owner);
        model = new ModelManager(roster, new UserPrefs());
        storage = new CountingStorage(folder.resolve("roster.json"));
        storage.saveAddressBook(roster);
        storage.attempts = 0;
        saved = Files.readAllBytes(storage.getAddressBookFilePath());
        logic = createLogic(model);
    }

    @Test
    public void execute_onlyMatchingContextChangesAndCompleteProfileSurvivesReload() throws Exception {
        logic.execute("find e9000002"); // owner is hidden
        var result = logic.execute(EDIT + " /team SEED-3 /section T14");
        Person expected = owner.withEnrolments(List.of(context("AY26/27 S1", "T14", "SEED-3"), SECOND));
        assertEquals("Updated enrolment for Alex Demo: CS2103T, AY26/27 S1.", result.getFeedbackToUser());
        assertEquals(expected, result.getSelectionTarget());
        assertEquals(List.of(expected, other), model.getFilteredPersonList());
        assertEquals(List.of(other, expected), storage.readAddressBook().orElseThrow().getPersonList());
        assertEquals(1, storage.attempts);
    }

    @Test
    public void execute_clearPreserveAndLiteralClearPersistIndependently() throws Exception {
        logic.execute(EDIT + " /team clear");
        var enrolment = updated().getEnrolments().getFirst();
        assertTrue(enrolment.getTeam().isEmpty());
        assertEquals(FIRST.getSection(), enrolment.getSection());
        logic.execute(EDIT + " /section clear /team Clear");
        enrolment = updated().getEnrolments().getFirst();
        assertTrue(enrolment.getSection().isEmpty());
        assertEquals("Clear", enrolment.getTeam().orElseThrow().value);
        assertEquals(updated(), storage.readAddressBook().orElseThrow().getPersonList().get(1));
        assertTrue(updated().isSample());
    }

    @Test
    public void execute_normalisedNoOpRevealsOwnerWithoutSavingButCaseOnlyLabelChangeSaves() throws Exception {
        logic.execute("find e9000002");
        var result = logic.execute(EDIT.replace("CS2103T", "cs2103t") + " /section Lab\t 2 /team SEED-2");
        assertEquals("No values changed.", result.getFeedbackToUser());
        assertEquals(owner, result.getSelectionTarget());
        assertEquals(List.of(owner, other), model.getFilteredPersonList());
        assertEquals(0, storage.attempts);
        assertArrayEquals(saved, Files.readAllBytes(storage.getAddressBookFilePath()));
        logic.execute(EDIT + " /section lab 2");
        assertEquals(1, storage.attempts);
        assertEquals("lab 2", updated().getEnrolments().getFirst().getSection().orElseThrow().value);
        logic.execute(EDIT + " /section clear");
        byte[] cleared = Files.readAllBytes(storage.getAddressBookFilePath());
        result = logic.execute(EDIT + " /section clear");
        assertEquals("No values changed.", result.getFeedbackToUser());
        assertEquals(2, storage.attempts);
        assertArrayEquals(cleared, Files.readAllBytes(storage.getAddressBookFilePath()));
    }

    @Test
    public void execute_unknownOwnerOrContextNeverCreatesRecords() {
        CommandException missing = assertThrows(CommandException.class, () ->
                logic.execute(EDIT.replace("e9000001", "e9999999") + " /team X"));
        assertEquals("No student found with NUS email: e9999999@u.nus.edu.", missing.getMessage());
        missing = assertThrows(CommandException.class, () ->
                logic.execute(EDIT.replace("CS2103T", "CS2113T") + " /team X"));
        assertEquals("No matching enrolment found for e9000001@u.nus.edu, CS2113T, AY26/27 S1. "
                + "Use enrol to add it first.", missing.getMessage());
        assertEquals(owner, updated());
        assertEquals(0, storage.attempts);
    }

    @Test
    public void execute_invalidSecondFieldRejectsValidFirstFieldAndNoOpsStillValidate() throws Exception {
        for (String command : new String[] {EDIT + " /section T14 /team ___", EDIT + " /section Lab 2 /team ___"}) {
            assertThrows(ParseException.class, () -> logic.execute(command));
            assertEquals(owner, updated());
            assertArrayEquals(saved, Files.readAllBytes(storage.getAddressBookFilePath()));
        }
        assertEquals(0, storage.attempts);
    }

    @Test
    public void execute_atomicReplacementFailureRestoresRosterFilterOrderBytesAndClearsPending() throws Exception {
        logic.execute("delete e9000001@u.nus.edu");
        model.updateFilteredPersonList(person -> true,
                Comparator.comparing((Person p) -> p.getEmail().value).reversed());
        List<Person> displayed = List.copyOf(model.getFilteredPersonList());
        storage.failReplace = true;
        CommandException error = assertThrows(CommandException.class, () -> logic.execute(EDIT + " /team NEW"));
        assertEquals(Messages.MESSAGE_PENDING_DELETION_CANCELLED + "\n" + EditEnrolCommand.MESSAGE_FAILURE,
                error.getMessage());
        assertEquals(owner, updated());
        assertEquals(displayed, model.getFilteredPersonList());
        assertFalse(logic.hasPendingDeletion());
        assertArrayEquals(saved, Files.readAllBytes(storage.getAddressBookFilePath()));
        try (var files = Files.list(folder)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().startsWith(".socdex-")));
        }
        storage.failReplace = false;
        logic.execute(EDIT + " /team NEW");
        assertEquals("NEW", updated().getEnrolments().getFirst().getTeam().orElseThrow().value);
    }

    @Test
    public void execute_presentationFailureNeverSavesAndRestoresPreviousResults() throws Exception {
        logic.execute("find e9000002");
        CommandException error = assertThrows(CommandException.class, () ->
                logic.execute(EDIT + " /team NEW", result -> {
                    throw new AssertionError("Injected profile rendering failure");
                }));
        assertEquals(EditEnrolCommand.MESSAGE_FAILURE, error.getMessage());
        assertEquals(owner, updated());
        assertEquals(List.of(other), model.getFilteredPersonList());
        assertEquals(0, storage.attempts);
        assertArrayEquals(saved, Files.readAllBytes(storage.getAddressBookFilePath()));
    }

    @Test
    public void execute_recoveryRejectsChangesAndNoOps() {
        LogicManager recovery = createLogic(new ModelManager(model.getAddressBook(), new UserPrefs(), true));
        for (String input : new String[] {EDIT + " /team NEW", EDIT + " /team SEED-2"}) {
            CommandException error = assertThrows(CommandException.class, () -> recovery.execute(input));
            assertEquals(LogicManager.MESSAGE_READ_ONLY, error.getMessage());
        }
        assertEquals(0, storage.attempts);
    }

    private LogicManager createLogic(ModelManager source) {
        return new LogicManager(source, new StorageManager(storage,
                new JsonUserPrefsStorage(folder.resolve("preferences.json"))));
    }

    private Person updated() {
        return model.getAddressBook().getPersonList().get(1);
    }

    private static Enrolment context(String semester, String section, String team) {
        return new Enrolment(new ModuleCode("CS2103T"), new Semester(semester),
                Optional.of(new Section(section)), Optional.of(new Team(team)));
    }

    private static class CountingStorage extends JsonAddressBookStorage {
        private int attempts;
        private boolean failReplace;

        CountingStorage(Path file) {
            super(file);
        }

        @Override
        public void saveAddressBook(ReadOnlyAddressBook roster) throws IOException {
            attempts++;
            super.saveAddressBook(roster);
        }

        @Override
        protected void replaceFile(Path temporary, Path target) throws IOException {
            if (failReplace) {
                throw new IOException("Injected atomic replacement failure");
            }
            super.replaceFile(temporary, target);
        }
    }
}
