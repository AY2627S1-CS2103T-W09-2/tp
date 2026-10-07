package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
            for (String command : new String[] {"delete 1", "clear", "edit 1 n/Changed Name", "remark 1 r/Changed",
                "add n/Fictional Student p/12345 e/demo@u.nus.edu a/Test"}) {
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
        for (String command : new String[] {"delete 1", "clear", "edit 1 n/Changed Name", "remark 1 r/Changed",
            "add n/Fictional Student p/12345 e/demo@u.nus.edu a/Test"}) {
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
        for (String command : new String[] {"list", "find Alice", "help", "exit", "edit 1 n/Alice Pauline"}) {
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
        for (String command : new String[] {"clear", "delete 1", "edit 1 n/Fictional", "remark 1 r/Test"}) {
            assertThrows(CommandException.class, LogicManager.MESSAGE_READ_ONLY, () -> logic.execute(command));
        }
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
}
