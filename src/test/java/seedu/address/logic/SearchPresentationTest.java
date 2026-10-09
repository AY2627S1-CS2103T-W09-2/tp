package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

public class SearchPresentationTest {
    @TempDir
    public Path temporaryFolder;

    @Test
    public void execute_failedPresentation_restoresPriorFilterOrderAndSavedData() throws Exception {
        ModelManager model = new ModelManager();
        Person mei = new PersonBuilder().withName("Mei Tan").withEmail("e9000003@u.nus.edu")
                .withTelegram("Mei_Handle").withGitHub("Mei-Repo").build();
        Person alex = new PersonBuilder().withName("Alex Tan").withEmail("e9000002@u.nus.edu")
                .withTelegram("Shared_Handle").withGitHub("Shared-Repo").build();
        Person otherAlex = new PersonBuilder().withName("alex tan").withEmail("e9000001@u.nus.edu")
                .withTelegram("Shared_Handle").withGitHub("Shared-Repo").build();
        model.addPerson(mei);
        model.addPerson(alex);
        model.addPerson(otherAlex);
        AtomicInteger saveAttempts = new AtomicInteger();
        JsonAddressBookStorage storage = new JsonAddressBookStorage(temporaryFolder.resolve("roster.json")) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook data) throws IOException {
                saveAttempts.incrementAndGet();
                super.saveAddressBook(data);
            }
        };
        storage.saveAddressBook(model.getAddressBook());
        byte[] originalBytes = Files.readAllBytes(storage.getAddressBookFilePath());
        Logic logic = new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))));

        for (String previousQuery : List.of("find missing", "find Mei",
                "find @Mei_Handle", "find Shared-Repo", "find Alex")) {
            logic.execute(previousQuery);
            List<Person> previousResults = List.copyOf(model.getFilteredPersonList());
            for (String nextQuery : List.of("find missing", "find Mei", "find Tan",
                    "find @Mei_Handle", "find Shared-Repo")) {
                CommandException failure = assertThrows(CommandException.class, () ->
                        logic.execute(nextQuery, result -> {
                            throw new IllegalStateException("Injected selection/layout failure");
                        }));
                assertEquals(Messages.MESSAGE_SEARCH_DISPLAY_FAILURE, failure.getMessage());
                assertEquals(previousResults, model.getFilteredPersonList());
                assertThrows(CommandException.class, () -> logic.execute(nextQuery, result -> {
                    throw new AssertionError("Injected FXML load failure");
                }));
                assertEquals(previousResults, model.getFilteredPersonList());
                assertEquals(List.of(mei, alex, otherAlex), model.getAddressBook().getPersonList());
                assertArrayEquals(originalBytes, Files.readAllBytes(storage.getAddressBookFilePath()));
            }
        }

        // Recovery retains the live filter and comparator rather than a stale copy of result rows.
        Person another = new PersonBuilder().withName("Alex Taylor").withEmail("e9000004@u.nus.edu").build();
        model.setPerson(mei, another);
        assertEquals(List.of(otherAlex, alex, another), model.getFilteredPersonList());

        // A retry can present and commit the new complete result normally.
        logic.execute("find e9000004", result -> {
            assertTrue(result.isUpdateSelection());
            assertEquals(List.of(another), model.getFilteredPersonList());
        });
        assertEquals(List.of(another), model.getFilteredPersonList());
        assertArrayEquals(originalBytes, Files.readAllBytes(storage.getAddressBookFilePath()));
        assertEquals(1, saveAttempts.get());
    }
}
