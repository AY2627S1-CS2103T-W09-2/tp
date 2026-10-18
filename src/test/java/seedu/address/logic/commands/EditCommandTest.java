package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.EditCommandParser;
import seedu.address.model.ModelManager;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.person.Person;
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

    @Test
    public void execute_identityChange_preservesOtherFieldsAndRetiresOldEmail() throws Exception {
        var original = new PersonBuilder().withName("Alex Tan").withEmail("alex@u.nus.edu").withSample(true)
                .withTelegram("Alex_Tan").withGitHub("alex-tan").withRemark("Retained").withTags("demo")
                .withEnrolments(new Enrolment(new ModuleCode("CS2103T"), new Semester("AY26/27 S1"))).build();
        ModelManager model = new ModelManager();
        model.addPerson(original);
        var parser = new EditCommandParser();
        var result = parser.parse(" /email ALEX@u.nus.edu /new-email e7654321@U.NUS.EDU /name Alex  Lim")
                .execute(model);
        Person expected = new PersonBuilder(original).withName("Alex Lim").withEmail("e7654321@u.nus.edu").build();
        assertEquals("Updated student: Alex Lim.", result.getFeedbackToUser());
        assertEquals(expected, result.getSelectionTarget());
        assertEquals(List.of(expected), model.getAddressBook().getPersonList());
        var oldEmail = parser.parse(" /email alex@u.nus.edu /github other");
        assertEquals("No student found with NUS email: alex@u.nus.edu.",
                assertThrows(CommandException.class, () -> oldEmail.execute(model)).getMessage());
    }

    @Test
    public void execute_replacementEmail_excludesTargetButRejectsOtherProfiles() throws Exception {
        var alex = new PersonBuilder().withName("Alex Tan").withEmail("alex@u.nus.edu").build();
        var mei = new PersonBuilder().withName("Mei Tan").withEmail("mei@u.nus.edu").build();
        ModelManager model = new ModelManager();
        model.addPerson(alex);
        model.addPerson(mei);
        var parser = new EditCommandParser();
        var conflict = parser.parse(" /email alex@u.nus.edu /name Alex Lim /new-email \tMEI@U.NUS.EDU");
        assertEquals(EditCommand.MESSAGE_DUPLICATE_EMAIL,
                assertThrows(CommandException.class, () -> conflict.execute(model)).getMessage());
        assertEquals(List.of(alex, mei), model.getAddressBook().getPersonList());
        // Lookup precedes the duplicate check.
        var absent = parser.parse(" /email absent@u.nus.edu /new-email mei@u.nus.edu");
        assertEquals("No student found with NUS email: absent@u.nus.edu.",
                assertThrows(CommandException.class, () -> absent.execute(model)).getMessage());
        // The target's own canonical email is not a conflict.
        var self = parser.parse(" /email alex@u.nus.edu /new-email ALEX@U.NUS.EDU");
        assertEquals(EditCommand.MESSAGE_NO_CHANGE, self.execute(model).getFeedbackToUser());
        assertEquals(List.of(alex, mei), model.getAddressBook().getPersonList());
        assertEquals("Updated student: Alex Lim.", parser.parse(
                " /email alex@u.nus.edu /new-email Alex@u.nus.edu /name Alex Lim").execute(model).getFeedbackToUser());
        assertEquals(List.of(new PersonBuilder(alex).withName("Alex Lim").build(), mei),
                model.getAddressBook().getPersonList());
    }
}
