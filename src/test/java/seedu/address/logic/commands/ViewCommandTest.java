package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Email;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

public class ViewCommandTest {
    private Model model;
    private Person alexOne;
    private Person alexTwo;
    private Person mei;

    @BeforeEach
    public void setUp() {
        model = new ModelManager();
        mei = new PersonBuilder().withName("Mei Lim").withEmail("e9000003@u.nus.edu").build();
        alexTwo = new PersonBuilder().withName("Alex Tan").withEmail("e9000002@u.nus.edu").build();
        alexOne = new PersonBuilder().withName("Alex Tan").withEmail("e9000001@u.nus.edu").build();
        model.addPerson(mei);
        model.addPerson(alexTwo);
        model.addPerson(alexOne);
    }

    @Test
    public void execute_visibleTarget_preservesCurrentResults() throws Exception {
        new FindCommand("Alex").execute(model);
        List<Person> results = List.copyOf(model.getFilteredPersonList());
        CommandResult result = view("e9000002@u.nus.edu").execute(model);
        assertEquals(CommandResult.forTarget("Viewing student: Alex Tan.", alexTwo), result);
        assertEquals(results, model.getFilteredPersonList());
    }

    @Test
    public void execute_hiddenTarget_revealsFullSortedRoster() throws Exception {
        new FindCommand("Mei").execute(model);
        CommandResult result = view("e9000001@u.nus.edu").execute(model);
        assertEquals(alexOne, result.getSelectionTarget());
        assertEquals(List.of(alexOne, alexTwo, mei), model.getFilteredPersonList());
    }

    @Test
    public void execute_sameNameStudents_selectsOnlyRequestedEmail() throws Exception {
        assertEquals(alexOne, view("e9000001@u.nus.edu").execute(model).getSelectionTarget());
        assertEquals(alexTwo, view("e9000002@u.nus.edu").execute(model).getSelectionTarget());
    }

    @Test
    public void execute_unknownEmail_rejectedWithCanonicalValueAndUnchangedResults() {
        new FindCommand("Mei").execute(model);
        AddressBook before = new AddressBook(model.getAddressBook());
        assertThrows(CommandException.class, "No student found with NUS email: e9000009@u.nus.edu.", () ->
                new ViewCommand(new Email("E9000009@U.NUS.EDU")).execute(model));
        assertEquals(List.of(mei), model.getFilteredPersonList());
        assertEquals(before, model.getAddressBook());
    }

    @Test
    public void execute_existingStudent_leavesRosterUnchangedAndIsReadOnly() throws Exception {
        AddressBook before = new AddressBook(model.getAddressBook());
        view("e9000003@u.nus.edu").execute(model);
        assertEquals(before, model.getAddressBook());
        assertTrue(view("e9000003@u.nus.edu").isReadOnly());
    }

    @Test
    public void getDisplayFailureMessage_usesProfileRetryGuidance() {
        assertEquals("Profile could not be displayed. Try viewing the student again.",
                view("e9000003@u.nus.edu").getDisplayFailureMessage("default"));
        assertEquals("default", new ListCommand().getDisplayFailureMessage("default"));
    }

    @Test
    public void equals() {
        ViewCommand command = view("e9000001@u.nus.edu");
        assertTrue(command.equals(command));
        assertTrue(command.equals(view("E9000001@U.NUS.EDU")));
        assertFalse(command.equals(view("e9000002@u.nus.edu")));
        assertFalse(command.equals(null));
        assertEquals(ViewCommand.class.getCanonicalName() + "{email=e9000001@u.nus.edu}", command.toString());
    }

    private static ViewCommand view(String email) {
        return new ViewCommand(new Email(email));
    }
}
