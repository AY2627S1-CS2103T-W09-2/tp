package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

public class StorageSafetyTest {
    @TempDir
    public Path folder;

    @Test
    public void execute_failedMutations_restoreRosterAndFilter() throws Exception {
        for (IOException failure : new IOException[] {new IOException("replacement failed"),
            new AccessDeniedException("read-only file")}) {
            for (String command : new String[] {"delete 1", "clear", "edit /email alice@u.nus.edu /github Changed",
                "remark 1 r/Changed",
                "student add /name Fictional Student /email demo@u.nus.edu"}) {
                ModelManager model = createModel();
                model.updateFilteredPersonList(ALICE::equals);
                AddressBook original = new AddressBook(model.getAddressBook());
                Logic logic = createLogic(model, new JsonAddressBookStorage(folder.resolve("roster.json")) {
                    @Override
                    public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                        throw failure;
                    }
                });
                assertThrows(CommandException.class, () -> logic.execute(command));
                assertEquals(original, model.getAddressBook());
                assertEquals(List.of(ALICE), model.getFilteredPersonList());
            }
        }
    }

    @Test
    public void execute_failedMutation_restoresSearchOrderAndLiveFilter() throws Exception {
        for (String command : new String[] {"delete 1", "clear", "edit /email alice@u.nus.edu /github Changed",
            "remark 1 r/Changed",
            "student add /name Fictional Student /email demo@u.nus.edu"}) {
            ModelManager model = createModel();
            model.updateFilteredPersonList(person -> person.equals(ALICE) || person.equals(BENSON),
                    Comparator.comparing((Person person) -> person.getName().fullName)
                            .reversed());
            AddressBook original = new AddressBook(model.getAddressBook());
            Logic logic = createLogic(model, new JsonAddressBookStorage(folder.resolve("roster.json")) {
                @Override
                public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                    throw new IOException("replacement failed");
                }
            });
            assertThrows(CommandException.class, () -> logic.execute(command));
            assertEquals(original, model.getAddressBook());
            assertEquals(List.of(BENSON, ALICE), model.getFilteredPersonList());
            model.deletePerson(BENSON);
            assertEquals(List.of(ALICE), model.getFilteredPersonList());
        }
    }

    @Test
    public void execute_readOnlyInvalidAndUnchangedCommands_doNotSave() throws Exception {
        ModelManager model = createModel();
        Logic logic = createLogic(model, new JsonAddressBookStorage(folder.resolve("roster.json")) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) {
                throw new AssertionError("An unchanged roster must not be saved.");
            }
        });
        AddressBook original = new AddressBook(model.getAddressBook());
        for (String command : new String[] {"list", "find Alice", "help", "exit",
            "edit /email alice@u.nus.edu /github clear"}) {
            logic.execute(command);
        }
        assertThrows(ParseException.class, () -> logic.execute("invalid"));
        assertThrows(CommandException.class, () -> logic.execute("delete 999"));
        assertEquals(original, model.getAddressBook());
    }

    @Test
    public void execute_successfulMutation_savedBeforeResultIsReturned() throws Exception {
        ModelManager model = createModel();
        JsonAddressBookStorage storage = new JsonAddressBookStorage(folder.resolve("roster.json"));
        Logic logic = createLogic(model, storage);
        logic.execute("delete 1");
        assertEquals(List.of(BENSON), storage.readAddressBook().orElseThrow().getPersonList());
        assertEquals(model.getAddressBook(), storage.readAddressBook().orElseThrow());
    }

    @Test
    public void execute_recoveryRejectsAllMutationCommands() {
        ModelManager model = new ModelManager(new AddressBook(), new UserPrefs(), true);
        Logic logic = createLogic(model, new JsonAddressBookStorage(folder.resolve("roster.json")));
        for (String command : new String[] {"clear", "delete 1", "edit /email alice@u.nus.edu /github Fictional",
            "remark 1 r/Test"}) {
            assertThrows(CommandException.class, LogicManager.MESSAGE_READ_ONLY, () -> logic.execute(command));
        }
    }

    @Test
    public void execute_duplicateAndCanonicalNoOpEmails_doNotSave() throws Exception {
        assertEquals("alice@u.nus.edu", ALICE.getEmail().value); // the commands below use this email
        ModelManager model = createModel();
        Path rosterFile = folder.resolve("roster.json");
        new JsonAddressBookStorage(rosterFile).saveAddressBook(model.getAddressBook());
        byte[] originalBytes = Files.readAllBytes(rosterFile);
        AddressBook original = new AddressBook(model.getAddressBook());
        CountingStorage storage = new CountingStorage(rosterFile);
        Logic logic = createLogic(model, storage);

        // duplicate creation with the existing email in another case
        assertThrows(CommandException.class, AddCommand.MESSAGE_DUPLICATE_PERSON, () ->
                logic.execute("student add /name Mei Lim /email ALICE@U.NUS.EDU"));
        assertNothingSaved(storage, model, original, rosterFile, originalBytes);

        // a profile edited to its own email in another case changes no stored value
        logic.execute("edit /email ALICE@U.NUS.EDU /github clear");
        assertNothingSaved(storage, model, original, rosterFile, originalBytes);
    }

    @Test
    public void execute_canonicalisedEmailChanges_savedOnceAndReloaded() throws Exception {
        ModelManager model = createModel();
        Path rosterFile = folder.resolve("roster.json");
        CountingStorage storage = new CountingStorage(rosterFile);
        Logic logic = createLogic(model, storage);

        logic.execute("student add /name Mei Lim /email \tMEI@U.NUS.EDU\t");
        assertEquals(1, storage.saves);
        assertEquals("mei@u.nus.edu", findReloadedByName(model, rosterFile, "Mei Lim").getEmail().value);

        Person mei = findByName(model.getAddressBook(), "Mei Lim");
        logic.execute("edit /email MEI@U.NUS.EDU /github Mei-Lim");
        assertEquals(2, storage.saves);
        assertEquals("Mei-Lim", findReloadedByName(model, rosterFile, "Mei Lim").getGitHub().orElseThrow().value);
        assertEquals(storage.saves, storage.attempts);
    }

    @Test
    public void execute_failedEmailSave_rollsBackAndRetrySucceeds() throws Exception {
        ModelManager model = createModel();
        Path rosterFile = folder.resolve("roster.json");
        new JsonAddressBookStorage(rosterFile).saveAddressBook(model.getAddressBook());
        byte[] originalBytes = Files.readAllBytes(rosterFile);
        AddressBook original = new AddressBook(model.getAddressBook());
        model.updateFilteredPersonList(person -> person.equals(ALICE) || person.equals(BENSON),
                Comparator.comparing((Person person) -> person.getName().fullName).reversed());
        CountingStorage storage = new CountingStorage(rosterFile);
        storage.failNextAttempt = true;
        Logic logic = createLogic(model, storage);
        String add = "student add /name Mei Lim /email MEI@U.NUS.EDU";

        assertThrows(CommandException.class,
                "The student could not be saved. No data was changed.", () ->
                logic.execute(add));
        assertEquals(1, storage.attempts);
        assertEquals(0, storage.saves);
        assertEquals(original, model.getAddressBook());
        assertEquals(List.of(BENSON, ALICE), model.getFilteredPersonList());
        assertArrayEquals(originalBytes, Files.readAllBytes(rosterFile));

        // the retry succeeds, so the failed attempt left no duplicate behind
        logic.execute(add);
        assertEquals(2, storage.attempts);
        assertEquals(1, storage.saves);
        assertEquals(3, model.getAddressBook().getPersonList().size());
        assertEquals("mei@u.nus.edu", findReloadedByName(model, rosterFile, "Mei Lim").getEmail().value);
    }

    private ModelManager createModel() {
        ModelManager model = new ModelManager();
        model.addPerson(ALICE);
        model.addPerson(BENSON);
        return model;
    }

    private Logic createLogic(ModelManager model, JsonAddressBookStorage storage) {
        return new LogicManager(model,
                new StorageManager(storage, new JsonUserPrefsStorage(folder.resolve("prefs.json"))));
    }

    /**
     * Returns the one-based displayed index of {@code person}.
     */
    private static int displayedIndexOf(ModelManager model, Person person) {
        int index = model.getFilteredPersonList().indexOf(person);
        assertTrue(index >= 0);
        return index + 1;
    }

    private static Person findByName(ReadOnlyAddressBook roster, String name) {
        return roster.getPersonList().stream()
                .filter(person -> person.getName().fullName.equals(name))
                .findFirst().orElseThrow();
    }

    /**
     * Reloads the roster file, checks that it equals the roster in the model, and returns the named person.
     */
    private static Person findReloadedByName(ModelManager model, Path rosterFile, String name) throws Exception {
        ReadOnlyAddressBook reloaded = new JsonAddressBookStorage(rosterFile).readAddressBook().orElseThrow();
        assertEquals(model.getAddressBook(), new AddressBook(reloaded));
        return findByName(reloaded, name);
    }

    private static void assertNothingSaved(CountingStorage storage, ModelManager model, AddressBook original,
            Path rosterFile, byte[] originalBytes) throws IOException {
        assertEquals(0, storage.attempts);
        assertEquals(original, model.getAddressBook());
        assertArrayEquals(originalBytes, Files.readAllBytes(rosterFile));
    }

    /**
     * Counts roster save attempts and completed saves; can fail the next attempt before anything is written.
     */
    private static class CountingStorage extends JsonAddressBookStorage {
        static final String FAILURE_MESSAGE = "replacement failed";

        private int attempts;
        private int saves;
        private boolean failNextAttempt;

        CountingStorage(Path filePath) {
            super(filePath);
        }

        @Override
        public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
            attempts++;
            if (failNextAttempt) {
                failNextAttempt = false;
                throw new IOException(FAILURE_MESSAGE);
            }
            super.saveAddressBook(addressBook);
            saves++;
        }
    }
}
