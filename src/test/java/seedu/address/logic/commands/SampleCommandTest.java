package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

public class SampleCommandTest {

    @Test
    public void execute_emptyRoster_loadsCompleteSortedFixtureWithoutSelectionTarget() throws Exception {
        ModelManager model = new ModelManager();

        CommandResult result = new SampleCommand().execute(model);

        assertEquals(SampleCommand.MESSAGE_SUCCESS, result.getFeedbackToUser());
        assertTrue(result.isUpdateSelection());
        assertNull(result.getSelectionTarget());
        assertEquals(List.of("Alex Tan", "Alex Tan", "Mei Lim", "Nur Aisyah", "Ravi Kumar"),
                model.getFilteredPersonList().stream().map(person -> person.getName().fullName).toList());
        assertEquals(List.of("e9000001@u.nus.edu", "e9000002@u.nus.edu", "e9000003@u.nus.edu",
                "e9000005@u.nus.edu", "e9000004@u.nus.edu"),
                model.getFilteredPersonList().stream().map(person -> person.getEmail().value).toList());
        assertTrue(model.getAddressBook().getPersonList().stream().allMatch(Person::isSample));
        assertEquals(5, model.getAddressBook().getPersonList().stream()
                .mapToInt(person -> person.getEnrolments().size()).sum());
    }

    @Test
    public void execute_nonEmptyRoster_rejectsWithoutCallingProviderOrChangingData() {
        ModelManager model = new ModelManager();
        Person existing = new PersonBuilder().withName("Real Student").withEmail("real@u.nus.edu").build();
        model.addPerson(existing);
        SampleCommand command = new SampleCommand(() -> {
            throw new AssertionError("The fixture must not be prepared for a non-empty roster.");
        });

        CommandException error = assertThrows(CommandException.class, () -> command.execute(model));

        assertEquals(SampleCommand.MESSAGE_NON_EMPTY, error.getMessage());
        assertEquals(List.of(existing), model.getAddressBook().getPersonList());
    }

    @Test
    public void execute_invalidFixture_rejectsBeforeChangingRoster() {
        ModelManager model = new ModelManager();
        SampleCommand incomplete = new SampleCommand(AddressBook::new);
        SampleCommand malformed = new SampleCommand(() -> {
            throw new IllegalArgumentException("Invalid fixture value");
        });

        for (SampleCommand command : List.of(incomplete, malformed)) {
            CommandException error = assertThrows(CommandException.class, () -> command.execute(model));
            assertEquals(SampleCommand.MESSAGE_INVALID_FIXTURE, error.getMessage());
            assertTrue(model.getAddressBook().getPersonList().isEmpty());
        }
    }

    @Test
    public void equals_statelessCommands_equal() {
        assertEquals(new SampleCommand(), new SampleCommand());
    }
}
