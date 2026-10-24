package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import org.junit.jupiter.api.Test;

import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;

public class ClearCommandTest {

    @Test
    public void execute_emptyAddressBook_rejected() {
        Model model = new ModelManager();

        assertCommandFailure(new ClearCommand(), model, ClearCommand.MESSAGE_RETIRED);
    }

    @Test
    public void execute_nonEmptyAddressBook_rejected() {
        Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());

        assertCommandFailure(new ClearCommand(), model, ClearCommand.MESSAGE_RETIRED);
        assertTrue(new ClearCommand().isReadOnly());
    }

}
