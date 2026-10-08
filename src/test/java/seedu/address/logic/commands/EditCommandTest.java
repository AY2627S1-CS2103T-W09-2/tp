package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.EditCommandParser;
import seedu.address.model.ModelManager;
import seedu.address.testutil.PersonBuilder;

public class EditCommandTest {
    @Test
    public void execute_updateAndNoOp_preserveRetainedFieldsAndSelectTarget() throws Exception {
        var original = new PersonBuilder().withSample(true).withRemark("Retained")
                .withTags("friend").withTelegram("Amy_Bee").build();
        ModelManager model = new ModelManager();
        model.addPerson(original);
        model.updateFilteredPersonList(person -> false);
        var command = new EditCommandParser().parse(" /email " + original.getEmail() + " /github amy-bee");
        var result = command.execute(model);
        var updated = model.getFilteredPersonList().get(0);
        assertEquals(original.getName(), updated.getName());
        assertEquals(original.getTags(), updated.getTags());
        assertEquals(original.getRemark(), updated.getRemark());
        assertEquals(original.getTelegram(), updated.getTelegram());
        assertEquals(true, updated.isSample());
        assertEquals(updated, result.getSelectionTarget());
        assertEquals("Updated student: Amy Bee.", result.getFeedbackToUser());
        assertEquals(EditCommand.MESSAGE_NO_CHANGE, command.execute(model).getFeedbackToUser());
    }

    @Test
    public void execute_missingTarget_rejected() throws Exception {
        var command = new EditCommandParser().parse(" /email absent@u.nus.edu /github a");
        assertEquals("No student found with NUS email: absent@u.nus.edu.",
                assertThrows(CommandException.class, () -> command.execute(new ModelManager())).getMessage());
    }
}
