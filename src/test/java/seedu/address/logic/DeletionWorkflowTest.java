package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.ClearSamplesCommand;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.ConfirmDeleteCommand;
import seedu.address.logic.commands.ExitCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.commands.exceptions.DuplicateStudentException;
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

/**
 * Exercises the two-command deletion flow through {@code LogicManager} and the real JSON storage: preview,
 * confirmation, cancellation by other submissions, rollback after save or display failure, and recovery mode.
 */
public class DeletionWorkflowTest {
    private static final String PREVIEW_REAL = "delete e9000011@u.nus.edu";
    private static final String CONFIRM_REAL = "confirm-delete e9000011@u.nus.edu";
    private static final String PREVIEW_SAMPLE = "delete e9000001@u.nus.edu";
    private static final String CONFIRM_SAMPLE = "confirm-delete e9000001@u.nus.edu";
    private static final String CANCELLED = Messages.MESSAGE_PENDING_DELETION_CANCELLED;

    @TempDir
    public Path folder;

    // Same name and identical enrolments: only the canonical email distinguishes the two Alex Tan profiles.
    private final Person sampleAlex = new PersonBuilder().withName("Alex Tan").withEmail("e9000001@u.nus.edu")
            .withSample(true).withEnrolments(enrolment("CS2103T", "AY26/27 S1"), enrolment("CS2113T", "AY26/27 S2"))
            .build();
    private final Person realAlex = new PersonBuilder().withName("Alex Tan").withEmail("e9000011@u.nus.edu")
            .withTelegram("Real_Alex").withGitHub("real-alex").withRemark("Kept").withTags("tutor")
            .withEnrolments(enrolment("CS2103T", "AY26/27 S1"), enrolment("CS2113T", "AY26/27 S2")).build();
    private final Person mei = new PersonBuilder().withName("Mei Lim").withEmail("e9000003@u.nus.edu")
            .withSample(true).withEnrolments(enrolment("CS2103T", "AY26/27 S1")).build();

    private ModelManager model;
    private CountingStorage storage;
    private Logic logic;
    private byte[] savedBytes;

    @BeforeEach
    public void setUp() throws IOException {
        model = createModel(false);
        storage = new CountingStorage(folder.resolve("main").resolve("roster.json"));
        logic = createLogic(model, storage);
        storage.saveAddressBook(model.getAddressBook());
        storage.attempts = 0;
        savedBytes = Files.readAllBytes(storage.getAddressBookFilePath());
    }

    @Test
    public void execute_preview_presentsTargetWithoutSaving() throws Exception {
        AtomicReference<CommandResult> presented = new AtomicReference<>();
        CommandResult result = logic.execute(PREVIEW_REAL, presented::set);

        assertEquals(realAlex, result.getSelectionTarget());
        assertEquals(result, presented.get());
        assertEquals(Optional.of(realAlex.getEmail()), model.getPendingDeletion());
        assertNothingSaved();
        assertEquals(List.of(mei, realAlex, sampleAlex), model.getAddressBook().getPersonList());
    }

    @Test
    public void execute_confirmSample_presentsClearedRosterBeforeSavingOnlyTheRemoval() throws Exception {
        logic.execute(PREVIEW_SAMPLE);
        AtomicInteger attemptsWhenPresented = new AtomicInteger(-1);
        AtomicReference<List<Person>> rowsWhenPresented = new AtomicReference<>();
        AtomicReference<CommandResult> presented = new AtomicReference<>();

        CommandResult result = logic.execute(CONFIRM_SAMPLE, shown -> {
            attemptsWhenPresented.set(storage.attempts);
            rowsWhenPresented.set(List.copyOf(model.getFilteredPersonList()));
            presented.set(shown);
        });

        assertEquals(CommandResult.forClearedSelection(
                "Deleted student: Alex Tan. NUS email: e9000001@u.nus.edu. Enrolments removed: 2."), result);
        assertEquals(result, presented.get());
        assertEquals(0, attemptsWhenPresented.get()); // presentation precedes the save, inside the transaction
        assertEquals(List.of(realAlex, mei), rowsWhenPresented.get());
        assertEquals(1, storage.attempts);
        assertRestartedRosterIs(List.of(mei, realAlex));
    }

    @Test
    public void execute_confirmNonSample_keepsSameNameSampleCompleteAfterRestart() throws Exception {
        logic.execute(PREVIEW_REAL);
        logic.execute(CONFIRM_REAL);
        assertRestartedRosterIs(List.of(mei, sampleAlex));
    }

    @Test
    public void execute_otherSubmissionWhilePending_cancelsWithNoticeAndOtherwiseUnchangedOutcome() throws Exception {
        String[] submissions = {
            "find Mei", "view e9000003@u.nus.edu", "list", "help", "exit",
            "edit /email e9000003@u.nus.edu /github Mei-Lim", "student add /name Zed Lim /email e9000099@u.nus.edu",
            "", " \t ", "unknowncommand", "find", "delete", "delete 1", "delete e9000011@u.nus.edu extra",
            "delete e9000099@u.nus.edu", "confirm-delete", "confirm-delete yes",
            "confirm-delete e9000011@u.nus.edu extra", "student add /name Mei Lim /email e9000003@u.nus.edu"
        };
        for (String submission : submissions) {
            setUp();
            Outcome expected = Outcome.of(createLogic(createModel(false),
                    new CountingStorage(folder.resolve("baseline").resolve("roster.json"))), submission);
            logic.execute(PREVIEW_REAL);

            Outcome actual = Outcome.of(logic, submission);

            assertEquals(expected.withNotice(), actual, submission);
            assertTrue(model.getPendingDeletion().isEmpty(), submission);
            assertThrows(CommandException.class, ConfirmDeleteCommand.MESSAGE_NO_PENDING, () ->
                    logic.execute(CONFIRM_REAL));
            assertTrue(model.hasPerson(realAlex), submission);
        }
    }

    @Test
    public void execute_duplicateCreationWhilePending_keepsRevealedProfileAndExceptionType() throws Exception {
        logic.execute(PREVIEW_REAL);
        AtomicReference<CommandResult> presented = new AtomicReference<>();

        DuplicateStudentException error = org.junit.jupiter.api.Assertions.assertThrows(
                DuplicateStudentException.class, () ->
                        logic.execute("student add /name Mei Lim /email e9000003@u.nus.edu", presented::set));

        assertTrue(error.getMessage().startsWith(CANCELLED + "\n"));
        assertEquals(mei, error.getExisting());
        assertNotNull(error.getCause());
        assertEquals(mei, presented.get().getSelectionTarget());
        assertTrue(model.getPendingDeletion().isEmpty());
    }

    @Test
    public void execute_secondPreview_replacesFirstAndMismatchedConfirmationDeletesNothing() throws Exception {
        logic.execute(PREVIEW_REAL);
        Outcome expectedPreview = Outcome.of(createLogic(createModel(false),
                new CountingStorage(folder.resolve("baseline").resolve("roster.json"))), "delete e9000003@u.nus.edu");

        assertEquals(expectedPreview.withNotice(), Outcome.of(logic, "delete e9000003@u.nus.edu"));
        assertEquals(Optional.of(mei.getEmail()), model.getPendingDeletion());

        // A valid confirmation is not cancelled by another command, so it carries no cancellation notice.
        assertThrows(CommandException.class, ConfirmDeleteCommand.MESSAGE_MISMATCH, () -> logic.execute(CONFIRM_REAL));
        assertThrows(CommandException.class, ConfirmDeleteCommand.MESSAGE_NO_PENDING, () ->
                logic.execute("confirm-delete e9000003@u.nus.edu"));
        assertEquals(List.of(mei, realAlex, sampleAlex), model.getAddressBook().getPersonList());
        assertNothingSaved();
    }

    @Test
    public void execute_confirmationWithoutPreview_rejectedWithoutNotice() {
        assertThrows(CommandException.class, ConfirmDeleteCommand.MESSAGE_NO_PENDING, () ->
                logic.execute(CONFIRM_REAL));
        assertNothingSaved();
    }

    @Test
    public void execute_confirmationSaveFailure_restoresEverythingAndRequiresFreshPreview() throws Exception {
        for (FailurePoint failurePoint : FailurePoint.values()) {
            setUp();
            logic.execute("find Alex");
            List<Person> results = List.copyOf(model.getFilteredPersonList());
            AddressBook before = new AddressBook(model.getAddressBook());
            logic.execute(PREVIEW_SAMPLE);
            failurePoint.inject(storage);

            assertThrows(CommandException.class, ConfirmDeleteCommand.MESSAGE_FAILURE, () ->
                    logic.execute(CONFIRM_SAMPLE));

            assertEquals(1, storage.attempts, failurePoint.name());
            assertArrayEquals(savedBytes, Files.readAllBytes(storage.getAddressBookFilePath()), failurePoint.name());
            assertNoTemporaryFiles();
            assertEquals(before, model.getAddressBook(), failurePoint.name());
            assertEquals(results, model.getFilteredPersonList(), failurePoint.name());
            assertTrue(model.getPendingDeletion().isEmpty(), failurePoint.name());
            assertThrows(CommandException.class, ConfirmDeleteCommand.MESSAGE_NO_PENDING, () ->
                    logic.execute(CONFIRM_SAMPLE));

            // Positive control: with storage healthy again, a fresh preview and confirmation succeed.
            storage.clearFailures();
            logic.execute(PREVIEW_SAMPLE);
            logic.execute(CONFIRM_SAMPLE);
            assertRestartedRosterIs(List.of(mei, realAlex));
        }
    }

    @Test
    public void execute_confirmationDisplayFailure_rollsBackWithoutSaving() throws Exception {
        for (RuntimeException displayFailure : new RuntimeException[] {
            new IllegalStateException("Injected layout failure"), new IllegalArgumentException("Injected target")}) {
            setUp();
            logic.execute("find Alex");
            List<Person> results = List.copyOf(model.getFilteredPersonList());
            logic.execute(PREVIEW_SAMPLE);

            assertThrows(CommandException.class, ConfirmDeleteCommand.MESSAGE_FAILURE, () ->
                    logic.execute(CONFIRM_SAMPLE, shown -> {
                        throw displayFailure;
                    }));

            assertNothingSaved();
            assertEquals(List.of(mei, realAlex, sampleAlex), model.getAddressBook().getPersonList());
            assertEquals(results, model.getFilteredPersonList());
            assertTrue(model.getPendingDeletion().isEmpty());
            assertThrows(CommandException.class, ConfirmDeleteCommand.MESSAGE_NO_PENDING, () ->
                    logic.execute(CONFIRM_SAMPLE));
        }
    }

    @Test
    public void execute_previewDisplayFailure_leavesNothingPending() throws Exception {
        logic.execute("find Mei");
        List<Person> results = List.copyOf(model.getFilteredPersonList());

        assertThrows(CommandException.class, Messages.MESSAGE_PROFILE_DISPLAY_FAILURE, () ->
                logic.execute(PREVIEW_SAMPLE, shown -> {
                    throw new AssertionError("Injected FXML load failure");
                }));

        assertEquals(results, model.getFilteredPersonList());
        assertTrue(model.getPendingDeletion().isEmpty());
        assertThrows(CommandException.class, ConfirmDeleteCommand.MESSAGE_NO_PENDING, () ->
                logic.execute(CONFIRM_SAMPLE));
        assertNothingSaved();
    }

    @Test
    public void pendingDeletionFacade_cancelsSessionStateWithoutSavingOrChangingResults() throws Exception {
        assertFalse(logic.hasPendingDeletion());
        assertFalse(logic.cancelPendingDeletion());

        logic.execute("find Alex");
        List<Person> results = List.copyOf(model.getFilteredPersonList());
        logic.execute(PREVIEW_SAMPLE);
        assertTrue(logic.hasPendingDeletion());
        assertTrue(logic.cancelPendingDeletion());
        assertFalse(logic.hasPendingDeletion());
        assertTrue(model.getPendingDeletion().isEmpty()); // the model remains the only source of truth
        assertEquals(results, model.getFilteredPersonList());
        assertEquals(List.of(mei, realAlex, sampleAlex), model.getAddressBook().getPersonList());
        assertThrows(CommandException.class, ConfirmDeleteCommand.MESSAGE_NO_PENDING, () ->
                logic.execute(CONFIRM_SAMPLE));
        assertNothingSaved();
    }

    @Test
    public void cancelPendingDeletionUnlessTarget_comparesCanonicalEmailOnly() throws Exception {
        assertFalse(logic.cancelPendingDeletionUnlessTarget(sampleAlex)); // nothing pending

        logic.execute("find Alex");
        List<Person> results = List.copyOf(model.getFilteredPersonList());
        logic.execute(PREVIEW_REAL);
        // The same student, even as a differently edited record, keeps the deletion pending.
        Person editedTarget = new PersonBuilder(realAlex).withRemark("Edited elsewhere").withTags().build();
        assertFalse(logic.cancelPendingDeletionUnlessTarget(realAlex));
        assertFalse(logic.cancelPendingDeletionUnlessTarget(editedTarget));
        assertTrue(logic.hasPendingDeletion());

        // A same-name student with another email is a different student.
        assertTrue(logic.cancelPendingDeletionUnlessTarget(sampleAlex));
        assertFalse(logic.hasPendingDeletion());
        assertFalse(logic.cancelPendingDeletionUnlessTarget(mei)); // later selection events change nothing

        // Returning to the original student does not recreate the deletion.
        assertFalse(logic.cancelPendingDeletionUnlessTarget(realAlex));
        assertThrows(CommandException.class, ConfirmDeleteCommand.MESSAGE_NO_PENDING, () ->
                logic.execute(CONFIRM_REAL));
        assertEquals(results, model.getFilteredPersonList());
        assertEquals(List.of(mei, realAlex, sampleAlex), model.getAddressBook().getPersonList());
        assertNothingSaved();
    }

    @Test
    public void execute_exitWhilePending_discardsDeletionAndKeepsExitFlag() throws Exception {
        for (String exit : new String[] {"bye", " \texit\t "}) {
            logic.execute(PREVIEW_REAL);

            CommandResult result = logic.execute(exit);

            assertEquals(CANCELLED + "\n" + ExitCommand.MESSAGE_EXIT_ACKNOWLEDGEMENT, result.getFeedbackToUser());
            assertTrue(result.isExit());
            assertFalse(logic.hasPendingDeletion());
            assertEquals(List.of(mei, realAlex, sampleAlex), model.getAddressBook().getPersonList());
        }
        assertNothingSaved();
    }

    @Test
    public void execute_exitWithParametersWhilePending_cancelsAndReportsUsageWithoutClosing() throws Exception {
        for (String exit : new String[] {"bye now", "exit 3", "bye\tnow"}) {
            logic.execute(PREVIEW_REAL);

            assertThrows(ParseException.class, CANCELLED + "\n" + ExitCommand.MESSAGE_USAGE, () ->
                    logic.execute(exit));

            assertFalse(logic.hasPendingDeletion());
            assertThrows(CommandException.class, ConfirmDeleteCommand.MESSAGE_NO_PENDING, () ->
                    logic.execute(CONFIRM_REAL));
            assertEquals(List.of(mei, realAlex, sampleAlex), model.getAddressBook().getPersonList());
        }
        assertNothingSaved();
    }

    @Test
    public void execute_exitInRecoverySession_succeedsWithoutTouchingTheFile() throws Exception {
        Logic recoveryLogic = createLogic(createModel(true), storage);
        for (String exit : new String[] {"bye", "exit"}) {
            CommandResult result = recoveryLogic.execute(exit);
            assertEquals(ExitCommand.MESSAGE_EXIT_ACKNOWLEDGEMENT, result.getFeedbackToUser());
            assertTrue(result.isExit());
        }
        assertNothingSaved();
    }

    @Test
    public void hasPendingDeletion_followsPreviewConfirmationCancellationAndFailure() throws Exception {
        logic.execute(PREVIEW_REAL);
        assertTrue(logic.hasPendingDeletion());
        logic.execute("list");
        assertFalse(logic.hasPendingDeletion());

        logic.execute(PREVIEW_REAL);
        assertThrows(CommandException.class, () -> logic.execute(CONFIRM_SAMPLE));
        assertFalse(logic.hasPendingDeletion());

        logic.execute(PREVIEW_REAL);
        logic.execute(CONFIRM_REAL);
        assertFalse(logic.hasPendingDeletion());
    }

    @Test
    public void execute_sampleClearWhilePending_cancelsAndRemovesOnlyClassifiedSamples() throws Exception {
        logic.execute(PREVIEW_REAL);
        AtomicReference<CommandResult> presented = new AtomicReference<>();

        CommandResult result = logic.execute("sample clear", presented::set);

        // Two classified samples (sample Alex Tan and Mei Lim) owning 2 + 1 enrolments are removed.
        assertEquals(CommandResult.forSurvivingSelection(String.format(ClearSamplesCommand.MESSAGE_SUCCESS, 2, 3))
                .withNotice(CANCELLED), result);
        assertTrue(result.isPreserveSurvivingSelection());
        assertFalse(result.isClearSelection());
        assertTrue(presented.get().isPreserveSurvivingSelection());
        assertFalse(logic.hasPendingDeletion());
        assertEquals(List.of(realAlex), model.getAddressBook().getPersonList());
        assertRestartedRosterIs(List.of(realAlex));
        assertThrows(CommandException.class, ConfirmDeleteCommand.MESSAGE_NO_PENDING, () ->
                logic.execute(CONFIRM_REAL));
        assertTrue(model.hasPerson(realAlex));
    }

    @Test
    public void execute_recoverySession_blocksPreviewAndConfirmationOfExistingStudent() {
        ModelManager recoveryModel = createModel(true);
        Logic recoveryLogic = createLogic(recoveryModel, storage);
        for (String command : new String[] {PREVIEW_SAMPLE, CONFIRM_SAMPLE}) {
            assertThrows(CommandException.class, LogicManager.MESSAGE_READ_ONLY, () -> recoveryLogic.execute(command));
        }
        assertTrue(recoveryModel.getPendingDeletion().isEmpty());
        assertTrue(recoveryModel.hasPerson(sampleAlex));
        assertNothingSaved();
    }

    @Test
    public void execute_restartAfterPreview_keepsStudentAndHasNothingPending() throws Exception {
        logic.execute(PREVIEW_SAMPLE);
        assertNothingSaved();
        ModelManager restarted = new ModelManager(storage.readAddressBook().orElseThrow(), new UserPrefs());
        assertTrue(restarted.hasPerson(sampleAlex));
        assertTrue(restarted.getPendingDeletion().isEmpty());
    }

    private ModelManager createModel(boolean isReadOnly) {
        AddressBook roster = new AddressBook();
        for (Person person : List.of(mei, realAlex, sampleAlex)) {
            roster.addPerson(person);
        }
        return new ModelManager(roster, new UserPrefs(), isReadOnly);
    }

    private Logic createLogic(ModelManager targetModel, JsonAddressBookStorage targetStorage) {
        return new LogicManager(targetModel, new StorageManager(targetStorage,
                new JsonUserPrefsStorage(folder.resolve("prefs.json"))));
    }

    private void assertNothingSaved() {
        assertEquals(0, storage.attempts);
        try {
            assertArrayEquals(savedBytes, Files.readAllBytes(storage.getAddressBookFilePath()));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private void assertNoTemporaryFiles() throws IOException {
        try (Stream<Path> files = Files.list(storage.getAddressBookFilePath().getParent())) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().startsWith(".socdex-")));
        }
    }

    /** Reloads the saved roster as a new session would and checks its records and empty deletion state. */
    private void assertRestartedRosterIs(List<Person> expected) throws Exception {
        ReadOnlyAddressBook reloaded = new JsonAddressBookStorage(storage.getAddressBookFilePath())
                .readAddressBook().orElseThrow();
        assertEquals(expected, reloaded.getPersonList());
        assertEquals(model.getAddressBook(), new AddressBook(reloaded));
        assertTrue(new ModelManager(reloaded, new UserPrefs()).getPendingDeletion().isEmpty());
    }

    private static Enrolment enrolment(String module, String semester) {
        return new Enrolment(new ModuleCode(module), new Semester(semester));
    }

    /** A command's complete outcome: either its result or its exception type and message. */
    private record Outcome(CommandResult result, Class<?> errorType, String errorMessage) {
        static Outcome of(Logic logic, String submission) {
            try {
                return new Outcome(logic.execute(submission), null, null);
            } catch (Exception e) {
                return new Outcome(null, e.getClass(), e.getMessage());
            }
        }

        Outcome withNotice() {
            if (result != null) {
                return new Outcome(result.withNotice(CANCELLED), null, null);
            }
            return new Outcome(null, errorType, CANCELLED + "\n" + errorMessage);
        }
    }

    /** Points in the existing atomic save at which a failure is injected. */
    private enum FailurePoint {
        WRITE_TEMPORARY_FILE {
            @Override
            void inject(CountingStorage target) {
                target.writeFailure = new IOException("Injected write failure");
            }
        },
        ATOMIC_REPLACE {
            @Override
            void inject(CountingStorage target) {
                target.replaceFailure = new IOException("Injected atomic move failure");
            }
        },
        PERMISSION_DENIED_ON_REPLACE {
            @Override
            void inject(CountingStorage target) {
                target.replaceFailure = new AccessDeniedException("Injected permission failure");
            }
        };

        abstract void inject(CountingStorage target);
    }

    /** Counts roster save attempts and can fail inside the real temporary-file write or atomic replacement. */
    private static class CountingStorage extends JsonAddressBookStorage {
        private int attempts;
        private IOException writeFailure;
        private IOException replaceFailure;

        CountingStorage(Path filePath) {
            super(filePath);
        }

        void clearFailures() {
            writeFailure = null;
            replaceFailure = null;
        }

        @Override
        public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
            attempts++;
            super.saveAddressBook(addressBook);
        }

        @Override
        protected void writeRoster(ReadOnlyAddressBook addressBook, Path temporary) throws IOException {
            if (writeFailure != null) {
                throw writeFailure;
            }
            super.writeRoster(addressBook, temporary);
        }

        @Override
        protected void replaceFile(Path temporary, Path target) throws IOException {
            if (replaceFailure != null) {
                throw replaceFailure;
            }
            super.replaceFile(temporary, target);
        }
    }
}
