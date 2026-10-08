package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.exceptions.DuplicateStudentException;
import seedu.address.model.ModelManager;
import seedu.address.testutil.PersonBuilder;

public class AddCommandTest {
    @Test
    public void constructor_nullPerson_rejected() {
        assertThrows(NullPointerException.class, () -> new AddCommand(null));
    }

    @Test
    public void execute_sameNameAndDuplicateEmail_identityAndSelection() throws Exception {
        ModelManager model = new ModelManager();
        var later = new PersonBuilder().withName("Alex Tan").withEmail("z@u.nus.edu").build();
        var earlier = new PersonBuilder(later).withEmail("a@u.nus.edu").build();
        new AddCommand(later).execute(model);
        var result = new AddCommand(earlier).execute(model);
        assertEquals("Added student: Alex Tan.", result.getFeedbackToUser());
        assertEquals(earlier, result.getSelectionTarget());
        assertEquals(java.util.List.of(earlier, later), model.getFilteredPersonList());
        var duplicate = new PersonBuilder(later).withName("Different").withEmail("Z@U.NUS.EDU").build();
        var error = assertThrows(DuplicateStudentException.class, () -> new AddCommand(duplicate).execute(model));
        assertEquals(later, error.getExisting());
        assertEquals(2, model.getAddressBook().getPersonList().size());
    }
}
