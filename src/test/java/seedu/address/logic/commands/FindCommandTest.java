package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

/**
 * Tests search against a complete roster, including repeated searches and display ordering.
 */
public class FindCommandTest {
    @Test
    public void execute_phrase_matchesContiguousNameOnly() {
        Model model = new ModelManager();
        Person alex = createPerson("Alex Tan", "e9000001@u.nus.edu");
        model.addPerson(createPerson("Alex Lim", "e9000002@u.nus.edu"));
        model.addPerson(createPerson("Mei Tan", "e9000003@u.nus.edu"));
        model.addPerson(alex);
        CommandResult result = new FindCommand("Alex Tan").execute(model);
        assertEquals(List.of(alex), model.getFilteredPersonList());
        assertEquals("1 student found for \"Alex Tan\".", result.getFeedbackToUser());
        assertTrue(result.isUpdateSelection());
    }

    @Test
    public void execute_repeatedSearches_searchesWholeRosterAndPreservesStoredOrder() {
        Model model = new ModelManager();
        Person mei = createPerson("Mei Tan", "e9000003@u.nus.edu");
        Person alexUpper = createPerson("Alex Tan", "e9000002@u.nus.edu");
        Person alexLower = createPerson("alex tan", "e9000001@u.nus.edu");
        model.addPerson(mei);
        model.addPerson(alexUpper);
        model.addPerson(alexLower);
        AddressBook before = new AddressBook(model.getAddressBook());

        CommandResult empty = new FindCommand("missing").execute(model);
        assertEquals(List.of(), model.getFilteredPersonList());
        assertEquals("No students found for \"missing\". Check the spelling or search with another identifier.",
                empty.getFeedbackToUser());
        assertTrue(empty.isUpdateSelection());

        new FindCommand("E9000003@").execute(model);
        assertEquals(List.of(mei), model.getFilteredPersonList());
        CommandResult multiple = new FindCommand("TAN").execute(model);
        assertEquals(List.of(alexLower, alexUpper, mei), model.getFilteredPersonList());
        assertEquals("3 students found for \"TAN\".", multiple.getFeedbackToUser());
        assertTrue(multiple.isUpdateSelection());
        assertEquals(before, model.getAddressBook());
    }

    @Test
    public void execute_identicalNames_returnsBothOrderedByEmail() {
        Model model = new ModelManager();
        Person laterAlex = createPerson("Alex Tan", "e9000002@u.nus.edu");
        Person earlierAlex = createPerson("Alex Tan", "e9000001@u.nus.edu");
        model.addPerson(laterAlex);
        model.addPerson(earlierAlex);
        model.addPerson(createPerson("Mei Tan", "e9000003@u.nus.edu"));
        AddressBook before = new AddressBook(model.getAddressBook());

        CommandResult result = new FindCommand("Alex Tan").execute(model);
        assertEquals(List.of(earlierAlex, laterAlex), model.getFilteredPersonList());
        assertEquals("2 students found for \"Alex Tan\".", result.getFeedbackToUser());
        assertEquals(before, model.getAddressBook());
    }

    @Test
    public void execute_bothFieldsMatch_returnsProfileOnce() {
        Model model = new ModelManager();
        Person alex = createPerson("Alex Tan", "alex@u.nus.edu");
        model.addPerson(alex);
        new FindCommand("alex").execute(model);
        assertEquals(List.of(alex), model.getFilteredPersonList());
    }

    @Test
    public void execute_sortedResults_deleteUsesDisplayedIndex() throws Exception {
        Model model = new ModelManager();
        Person mei = createPerson("Mei Tan", "e9000003@u.nus.edu");
        Person alex = createPerson("Alex Tan", "e9000001@u.nus.edu");
        model.addPerson(mei);
        model.addPerson(alex);
        new FindCommand("Tan").execute(model);
        assertEquals(List.of(alex, mei), model.getFilteredPersonList());
        new DeleteCommand(Index.fromOneBased(1)).execute(model);
        assertEquals(List.of(mei), model.getAddressBook().getPersonList());
    }

    @Test
    public void equalsAndToString() {
        FindCommand command = new FindCommand("Alex Tan");
        assertTrue(command.equals(command));
        assertEquals(command, new FindCommand("Alex Tan"));
        assertFalse(command.equals(new FindCommand("Mei Tan")));
        assertFalse(command.equals(null));
        assertFalse(command.equals("Alex Tan"));
        assertEquals(FindCommand.class.getCanonicalName() + "{query=Alex Tan}", command.toString());
    }

    private Person createPerson(String name, String email) {
        return new PersonBuilder().withName(name).withEmail(email).build();
    }
}
