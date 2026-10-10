package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.person.Email;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

/**
 * Contains integration tests (interaction with the Model) for the {@code DeleteCommand} preview.
 */
public class DeleteCommandTest {
    private Model model;
    private Person sampleAlex;
    private Person realAlex;
    private Person mei;

    @BeforeEach
    public void setUp() {
        model = new ModelManager();
        sampleAlex = new PersonBuilder().withName("Alex Tan").withEmail("e9000001@u.nus.edu").withSample(true)
                .withEnrolments(enrolment("CS2103T", "AY26/27 S1"), enrolment("CS2113T", "AY26/27 S2")).build();
        realAlex = new PersonBuilder().withName("Alex Tan").withEmail("e9000011@u.nus.edu").build();
        mei = new PersonBuilder().withName("Mei Lim").withEmail("e9000003@u.nus.edu")
                .withEnrolments(enrolment("CS2103T", "AY26/27 S1")).build();
        model.addPerson(mei);
        model.addPerson(realAlex);
        model.addPerson(sampleAlex);
    }

    @Test
    public void execute_visibleSampleTarget_previewsActualProfileAndKeepsSearch() throws Exception {
        new FindCommand("Alex").execute(model);
        List<Person> results = List.copyOf(model.getFilteredPersonList());
        AddressBook before = new AddressBook(model.getAddressBook());

        CommandResult result = preview("e9000001@u.nus.edu").execute(model);

        assertEquals(CommandResult.forTarget("Delete this student?\nAlex Tan\nNUS email: e9000001@u.nus.edu\n"
                + "Enrolments to remove: 2\nType: confirm-delete e9000001@u.nus.edu\n"
                + "Any other command will cancel this deletion.", sampleAlex), result);
        assertEquals(results, model.getFilteredPersonList());
        assertEquals(before, model.getAddressBook());
        assertEquals(Optional.of(sampleAlex.getEmail()), model.getPendingDeletion());
    }

    @Test
    public void execute_hiddenNonSampleTarget_revealsFullSortedRosterAndPreviewsZeroEnrolments() throws Exception {
        new FindCommand("Mei").execute(model);
        AddressBook before = new AddressBook(model.getAddressBook());

        CommandResult result = preview("e9000011@u.nus.edu").execute(model);

        assertEquals(CommandResult.forTarget("Delete this student?\nAlex Tan\nNUS email: e9000011@u.nus.edu\n"
                + "Enrolments to remove: 0\nType: confirm-delete e9000011@u.nus.edu\n"
                + "Any other command will cancel this deletion.", realAlex), result);
        assertEquals(List.of(sampleAlex, realAlex, mei), model.getFilteredPersonList());
        assertEquals(before, model.getAddressBook());
        assertEquals(Optional.of(realAlex.getEmail()), model.getPendingDeletion());
    }

    @Test
    public void execute_unknownEmail_rejectedWithoutPendingOrChange() {
        new FindCommand("Mei").execute(model);
        assertCommandFailure(new DeleteCommand(new Email("E9000009@U.NUS.EDU")), model,
                "No student found with NUS email: e9000009@u.nus.edu.");
        assertTrue(model.getPendingDeletion().isEmpty());
    }

    @Test
    public void execute_secondPreview_replacesPendingTarget() throws Exception {
        preview("e9000001@u.nus.edu").execute(model);
        preview("e9000003@u.nus.edu").execute(model);
        assertEquals(Optional.of(mei.getEmail()), model.getPendingDeletion());
    }

    @Test
    public void commandFlags_previewIsRecoveryGatedAndIsNotAConfirmation() {
        assertFalse(preview("e9000001@u.nus.edu").isReadOnly());
        assertFalse(preview("e9000001@u.nus.edu").isDeletionConfirmation());
    }

    @Test
    public void equalsAndToString() {
        DeleteCommand command = preview("e9000001@u.nus.edu");
        assertTrue(command.equals(command));
        assertTrue(command.equals(preview("E9000001@U.NUS.EDU")));
        assertEquals(command.hashCode(), preview("E9000001@U.NUS.EDU").hashCode());
        assertFalse(command.equals(preview("e9000002@u.nus.edu")));
        assertFalse(command.equals(new ConfirmDeleteCommand(new Email("e9000001@u.nus.edu"))));
        assertFalse(command.equals(null));
        assertEquals(DeleteCommand.class.getCanonicalName() + "{email=e9000001@u.nus.edu}", command.toString());
    }

    private static DeleteCommand preview(String email) {
        return new DeleteCommand(new Email(email));
    }

    private static Enrolment enrolment(String module, String semester) {
        return new Enrolment(new ModuleCode(module), new Semester(semester));
    }
}
