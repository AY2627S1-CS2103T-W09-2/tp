package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.logic.commands.CommandTestUtil.showPersonAtIndex;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

/**
 * Contains integration tests (interaction with the Model) and unit tests for ListCommand.
 */
public class ListCommandTest {

    private Model model;
    private Model expectedModel;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
    }

    @Test
    public void execute_listIsNotFiltered_showsSameList() {
        assertCommandSuccess(new ListCommand(), model, ListCommand.MESSAGE_SUCCESS, expectedModel);
    }

    @Test
    public void execute_listIsFiltered_showsEverything() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);
        assertCommandSuccess(new ListCommand(), model, ListCommand.MESSAGE_SUCCESS, expectedModel);
    }

    @Test
    public void execute_insertionOrderDiffers_showsNameThenEmailOrderWithoutChangingRoster() {
        Person zed = new PersonBuilder().withName("Zed Student").withEmail("zed@u.nus.edu").build();
        Person alexSecond = new PersonBuilder().withName("Alex Tan").withEmail("alex2@u.nus.edu").build();
        Person alexFirst = new PersonBuilder().withName("Alex Tan").withEmail("alex1@u.nus.edu").build();
        AddressBook insertionOrderedRoster = new AddressBook();
        insertionOrderedRoster.addPerson(zed);
        insertionOrderedRoster.addPerson(alexSecond);
        insertionOrderedRoster.addPerson(alexFirst);
        model = new ModelManager(insertionOrderedRoster, new UserPrefs());
        AddressBook rosterBeforeList = new AddressBook(model.getAddressBook());

        CommandResult result = new ListCommand().execute(model);

        assertEquals(new CommandResult(ListCommand.MESSAGE_SUCCESS), result);
        assertEquals(java.util.List.of(alexFirst, alexSecond, zed), model.getFilteredPersonList());
        assertEquals(rosterBeforeList, model.getAddressBook());
    }
}
