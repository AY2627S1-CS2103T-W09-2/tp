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

import seedu.address.logic.commands.ClearSamplesCommand;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.SampleCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonOrder;
import seedu.address.model.util.SampleDataUtil;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

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

    @Test
    public void execute_saveFailure_preservesExistingEmptyFileAndAllowsRetry() throws Exception {
        storage.saveAddressBook(model.getAddressBook());
        byte[] before = Files.readAllBytes(storage.getAddressBookFilePath());
        storage.fail = true;

        CommandException error = assertThrows(CommandException.class, () -> logic.execute("sample load"));

        assertEquals(SampleCommand.MESSAGE_SAVE_FAILURE, error.getMessage());
        assertArrayEquals(before, Files.readAllBytes(storage.getAddressBookFilePath()));
        assertTrue(model.getFilteredPersonList().isEmpty());
        storage.fail = false;
        logic.execute("sample load");
        assertEquals(5, storage.readAddressBook().orElseThrow().getPersonList().size());
    }

    @Test
    public void execute_displayFailure_rollsBackBeforeSaving() {
        assertThrows(CommandException.class, () -> logic.execute("sample load", result -> {
            throw new IllegalStateException("Injected presentation failure");
        }));
        assertEquals(0, storage.attempts);
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertFalse(Files.exists(storage.getAddressBookFilePath()));
    }

    @Test
    public void execute_editAndEnrolmentCopy_preservesSampleClassificationAfterReload() throws Exception {
        logic.execute("sample load");
        logic.execute("edit /email e9000005@u.nus.edu /telegram @nur_demo");
        Person nur = model.getAddressBook().getPersonList().stream()
                .filter(person -> person.getEmail().value.equals("e9000005@u.nus.edu")).findFirst().orElseThrow();
        assertTrue(nur.isSample());
        Person enrolled = nur.withEnrolments(List.of(new Enrolment(
                new ModuleCode("CS2103T"), new Semester("AY26/27 S1"))));
        model.setPerson(nur, enrolled);
        storage.saveAddressBook(model.getAddressBook());
        Person reloaded = storage.readAddressBook().orElseThrow().getPersonList().stream()
                .filter(person -> person.getEmail().equals(nur.getEmail())).findFirst().orElseThrow();
        assertEquals(enrolled, reloaded);
        assertTrue(reloaded.isSample());
        assertEquals("nur_demo", reloaded.getTelegram().orElseThrow().value);
        assertEquals(1, reloaded.getEnrolments().size());
    }

    @Test
    public void execute_clearMixedRoster_removesClassifiedSamplesAndPersistsActualCounts() throws Exception {
        Person realAlex = prepareAndSaveMixedRoster();
        int attemptsBeforeClear = storage.attempts;
        AtomicReference<CommandResult> presented = new AtomicReference<>();

        CommandResult result = logic.execute("sample\tclear", presented::set);

        assertEquals(String.format(ClearSamplesCommand.MESSAGE_SUCCESS, 5, 7), result.getFeedbackToUser());
        assertEquals(result, presented.get());
        assertTrue(result.isPreserveSurvivingSelection());
        assertEquals(attemptsBeforeClear + 1, storage.attempts);
        assertEquals(List.of(realAlex), model.getAddressBook().getPersonList());
        assertEquals(List.of(realAlex), model.getFilteredPersonList());
        assertEquals(List.of(realAlex), storage.readAddressBook().orElseThrow().getPersonList());

        byte[] clearedBytes = Files.readAllBytes(storage.getAddressBookFilePath());
        int attemptsAfterClear = storage.attempts;
        CommandResult repeated = logic.execute("sample clear");
        assertEquals(ClearSamplesCommand.MESSAGE_NO_SAMPLES, repeated.getFeedbackToUser());
        assertEquals(attemptsAfterClear, storage.attempts);
        assertArrayEquals(clearedBytes, Files.readAllBytes(storage.getAddressBookFilePath()));
        assertEquals(List.of(realAlex), model.getAddressBook().getPersonList());
    }

    @Test
    public void execute_clearWithoutSamples_preservesFilterAndDoesNotSave() throws Exception {
        Person alice = new PersonBuilder().withName("Alice Lim").withEmail("alice@u.nus.edu").build();
        Person bob = new PersonBuilder().withName("Bob Tan").withEmail("bob@u.nus.edu").build();
        model.addPerson(alice);
        model.addPerson(bob);
        storage.saveAddressBook(model.getAddressBook());
        logic.execute("find Bob");
        byte[] savedBytes = Files.readAllBytes(storage.getAddressBookFilePath());
        int attemptsBeforeClear = storage.attempts;
        AtomicReference<CommandResult> presented = new AtomicReference<>();

        CommandResult result = logic.execute("sample clear", presented::set);

        assertEquals(ClearSamplesCommand.MESSAGE_NO_SAMPLES, result.getFeedbackToUser());
        assertFalse(result.isUpdateSelection());
        assertNull(presented.get());
        assertEquals(List.of(bob), model.getFilteredPersonList());
        assertEquals(attemptsBeforeClear, storage.attempts);
        assertArrayEquals(savedBytes, Files.readAllBytes(storage.getAddressBookFilePath()));
    }

    @Test
    public void execute_clearSaveFailure_restoresRosterFilterAndFileBeforeRetry() throws Exception {
        Person realAlex = prepareAndSaveMixedRoster();
        logic.execute("find real.alex");
        AddressBook before = new AddressBook(model.getAddressBook());
        byte[] savedBytes = Files.readAllBytes(storage.getAddressBookFilePath());
        int attemptsBeforeClear = storage.attempts;
        storage.fail = true;

        CommandException error = assertThrows(CommandException.class, () -> logic.execute("sample clear"));

        assertEquals(ClearSamplesCommand.MESSAGE_SAVE_FAILURE, error.getMessage());
        assertEquals(attemptsBeforeClear + 1, storage.attempts);
        assertEquals(before, model.getAddressBook());
        assertEquals(List.of(realAlex), model.getFilteredPersonList());
        assertArrayEquals(savedBytes, Files.readAllBytes(storage.getAddressBookFilePath()));

        storage.fail = false;
        logic.execute("sample clear");
        assertEquals(List.of(realAlex), storage.readAddressBook().orElseThrow().getPersonList());
    }

    @Test
    public void execute_clearPresentationFailure_restoresRosterAndDoesNotSave() throws Exception {
        prepareAndSaveMixedRoster();
        AddressBook before = new AddressBook(model.getAddressBook());
        byte[] savedBytes = Files.readAllBytes(storage.getAddressBookFilePath());
        int attemptsBeforeClear = storage.attempts;

        assertThrows(CommandException.class, () -> logic.execute("sample clear", result -> {
            throw new IllegalStateException("Injected presentation failure");
        }));

        assertEquals(before, model.getAddressBook());
        assertEquals(attemptsBeforeClear, storage.attempts);
        assertArrayEquals(savedBytes, Files.readAllBytes(storage.getAddressBookFilePath()));
    }

    @Test
    public void execute_clearInReadOnlyRecovery_rejectsWithoutSaving() {
        ModelManager recoveryModel = new ModelManager(SampleDataUtil.getSampleAddressBook(), new UserPrefs(), true);
        LogicManager recoveryLogic = new LogicManager(recoveryModel,
                new StorageManager(storage, new JsonUserPrefsStorage(folder.resolve("clear-recovery-prefs.json"))));

        CommandException error = assertThrows(CommandException.class, () -> recoveryLogic.execute("sample clear"));

        assertEquals(LogicManager.MESSAGE_READ_ONLY, error.getMessage());
        assertEquals(0, storage.attempts);
        assertEquals(5, recoveryModel.getAddressBook().getPersonList().size());
    }

    private Person prepareAndSaveMixedRoster() throws Exception {
        model.setAddressBook(SampleDataUtil.getSampleAddressBook());
        Person nur = model.getAddressBook().getPersonList().stream()
                .filter(person -> person.getName().fullName.equals("Nur Aisyah"))
                .findFirst()
                .orElseThrow();
        Person editedSample = new PersonBuilder(nur)
                .withName("Edited Fictional Student")
                .withEmail("edited.sample@u.nus.edu")
                .withEnrolments(
                        new Enrolment(new ModuleCode("CS2103T"), new Semester("AY26/27 S1")),
                        new Enrolment(new ModuleCode("CS2100"), new Semester("AY26/27 S2")))
                .build();
        model.setPerson(nur, editedSample);
        Person realAlex = new PersonBuilder()
                .withName("Alex Tan")
                .withEmail("real.alex@u.nus.edu")
                .withTelegram("socdex_demo_alex")
                .withGitHub("socdex-demo-alex")
                .withEnrolments(new Enrolment(new ModuleCode("CS2103T"), new Semester("AY26/27 S1")))
                .build();
        model.addPerson(realAlex);
        model.updateFilteredPersonList(person -> person.equals(realAlex), PersonOrder.BY_NAME_THEN_EMAIL);
        storage.saveAddressBook(model.getAddressBook());
        return realAlex;
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
