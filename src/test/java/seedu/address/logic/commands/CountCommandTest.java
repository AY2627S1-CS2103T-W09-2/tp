package seedu.address.logic.commands;

import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.logic.commands.CommandTestUtil.showPersonAtIndex;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;

/** Contains integration tests for {@code CountCommand}. */
public class CountCommandTest {

    private Model model;
    private Model expectedModel;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
    }

    @Test
    public void execute_unfilteredList_reportsAllPersons() {
        int personCount = model.getFilteredPersonList().size();

        assertCommandSuccess(new CountCommand(), model,
                String.format(CountCommand.MESSAGE_SUCCESS, personCount), expectedModel);
    }

    @Test
    public void execute_filteredList_reportsShownPersons() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);
        expectedModel.updateFilteredPersonList(person -> person.equals(model.getFilteredPersonList().getFirst()));

        assertCommandSuccess(new CountCommand(), model,
                String.format(CountCommand.MESSAGE_SUCCESS, 1), expectedModel);
    }
}
