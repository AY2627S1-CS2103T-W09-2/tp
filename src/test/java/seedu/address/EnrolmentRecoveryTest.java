package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
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

class EnrolmentRecoveryTest {
    @TempDir
    private Path folder;

    @Test
    void invalidEnrolment_startupRecoveryPreservesFileThroughCommandsAndExit() throws Exception {
        Path target = folder.resolve("roster.json");
        JsonAddressBookStorage rosterStorage = new JsonAddressBookStorage(target);
        rosterStorage.saveAddressBook(roster());
        String invalid = Files.readString(target).replace("AY26/27 S1", "AY26/28 S1");
        Files.writeString(target, invalid);
        byte[] original = Files.readAllBytes(target);
        StorageManager storage = new StorageManager(rosterStorage,
                new JsonUserPrefsStorage(folder.resolve("prefs.json")));
        MainApp app = new MainApp();
        app.storage = storage;
        app.model = app.initModelManager(storage, new UserPrefs());
        app.logic = new LogicManager(app.model, storage);
        assertTrue(app.model.isReadOnly());
        app.logic.execute("find Nobody");
        app.logic.execute("list");
        assertThrows(CommandException.class, LogicManager.MESSAGE_READ_ONLY, () -> app.logic.execute("clear"));
        app.logic.execute("exit");
        app.stop();
        assertArrayEquals(original, Files.readAllBytes(target));
    }

    @Test
    void failedSave_restoresCompleteEnrolmentsFilterAndStoredBytes() throws Exception {
        Path target = folder.resolve("roster.json");
        JsonAddressBookStorage initialStorage = new JsonAddressBookStorage(target);
        AddressBook original = roster();
        initialStorage.saveAddressBook(original);
        byte[] saved = Files.readAllBytes(target);
        JsonAddressBookStorage failingStorage = new JsonAddressBookStorage(target) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook roster) throws IOException {
                throw new IOException("Injected enrolment save failure");
            }
        };
        StorageManager storage = new StorageManager(failingStorage,
                new JsonUserPrefsStorage(folder.resolve("prefs.json")));
        Model model = new MainApp().initModelManager(storage, new UserPrefs());
        LogicManager logic = new LogicManager(model, storage);
        logic.execute("find Amy");
        for (String command : new String[] {"edit /email e9000001@u.nus.edu /github Changed",
            "remark 1 r/Changed", "clear"}) {
            assertThrows(CommandException.class, () -> logic.execute(command));
            assertEquals(original, model.getAddressBook());
            assertEquals(original.getPersonList(), model.getFilteredPersonList());
            assertArrayEquals(saved, Files.readAllBytes(target));
            assertEquals(original, initialStorage.readAddressBook().orElseThrow());
        }

        // Deleting the enrolled student takes a preview, which does not save, and then a confirmation.
        logic.execute("delete e9000001@u.nus.edu");
        assertThrows(CommandException.class, () -> logic.execute("confirm-delete e9000001@u.nus.edu"));
        assertEquals(original, model.getAddressBook());
        assertEquals(original.getPersonList(), model.getFilteredPersonList());
        assertArrayEquals(saved, Files.readAllBytes(target));
        assertEquals(original, initialStorage.readAddressBook().orElseThrow());
    }

    private AddressBook roster() {
        Person person = new PersonBuilder().withEmail("e9000001@u.nus.edu").withEnrolments(
                new Enrolment(new ModuleCode("CS2103T"), new Semester("AY26/27 S1"))).build();
        AddressBook roster = new AddressBook();
        roster.addPerson(person);
        return roster;
    }
}
