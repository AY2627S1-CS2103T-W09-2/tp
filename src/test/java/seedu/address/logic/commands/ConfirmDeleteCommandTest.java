package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.person.Email;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

/**
 * Contains integration tests (interaction with the Model) for {@code ConfirmDeleteCommand}.
 */
public class ConfirmDeleteCommandTest {
    private Model model;
    private Person sampleAlex;
    private Person realAlex;
    private Person mei;

    @BeforeEach
    public void setUp() {
        model = new ModelManager();
        // Same name and identical enrolments: only the canonical email distinguishes the two Alex Tan profiles.
        sampleAlex = new PersonBuilder().withName("Alex Tan").withEmail("e9000001@u.nus.edu").withSample(true)
                .withEnrolments(enrolment("CS2103T", "AY26/27 S1"), enrolment("CS2113T", "AY26/27 S2")).build();
        realAlex = new PersonBuilder().withName("Alex Tan").withEmail("e9000011@u.nus.edu")
                .withTelegram("Real_Alex").withRemark("Kept").withTags("tutor")
                .withEnrolments(enrolment("CS2103T", "AY26/27 S1"), enrolment("CS2113T", "AY26/27 S2")).build();
        mei = new PersonBuilder().withName("Mei Lim").withEmail("e9000003@u.nus.edu").withSample(true).build();
        model.addPerson(mei);
        model.addPerson(realAlex);
        model.addPerson(sampleAlex);
    }

    @Test
    public void execute_pendingSampleTarget_removesOnlyItAndClearsSelection() throws Exception {
        model.setPendingDeletion(sampleAlex.getEmail());
        new FindCommand("Mei").execute(model);

        CommandResult result = confirm("e9000001@u.nus.edu").execute(model);

        assertEquals(CommandResult.forClearedSelection(
                "Deleted student: Alex Tan. NUS email: e9000001@u.nus.edu. Enrolments removed: 2."), result);
        assertEquals(List.of(mei, realAlex), model.getAddressBook().getPersonList());
        assertEquals(List.of(realAlex, mei), model.getFilteredPersonList());
        assertTrue(model.getPendingDeletion().isEmpty());
    }

    @Test
    public void execute_pendingNonSampleTarget_keepsSameNameSampleComplete() throws Exception {
        model.setPendingDeletion(realAlex.getEmail());

        CommandResult result = confirm("e9000011@u.nus.edu").execute(model);

        assertEquals("Deleted student: Alex Tan. NUS email: e9000011@u.nus.edu. Enrolments removed: 2.",
                result.getFeedbackToUser());
        assertEquals(List.of(mei, sampleAlex), model.getAddressBook().getPersonList());
        assertEquals(List.of(sampleAlex, mei), model.getFilteredPersonList());
    }

    @Test
    public void execute_oneStudentRemains_resultStillClearsSelection() throws Exception {
        model.deletePerson(realAlex);
        model.setPendingDeletion(sampleAlex.getEmail());

        CommandResult result = confirm("e9000001@u.nus.edu").execute(model);

        assertEquals(List.of(mei), model.getFilteredPersonList());
        assertTrue(result.isUpdateSelection());
        assertTrue(result.isClearSelection());
        assertNull(result.getSelectionTarget());
        // A search result would select the sole remaining student instead.
        assertNotEquals(CommandResult.forSearch(result.getFeedbackToUser()), result);
    }

    @Test
    public void execute_noPendingDeletion_rejectedWithoutChange() {
        assertCommandFailure(confirm("e9000001@u.nus.edu"), model,
                "There is no pending deletion. Use delete EMAIL first.");
    }

    @Test
    public void execute_differentValidEmail_consumesPendingAndPreservesBoth() {
        model.setPendingDeletion(sampleAlex.getEmail());
        assertCommandFailure(confirm("e9000011@u.nus.edu"), model,
                "Deletion was cancelled because the confirmation did not match the selected student.");
        assertTrue(model.getPendingDeletion().isEmpty());
        assertCommandFailure(confirm("e9000001@u.nus.edu"), model,
                "There is no pending deletion. Use delete EMAIL first.");
        assertEquals(List.of(mei, realAlex, sampleAlex), model.getAddressBook().getPersonList());
    }

    @Test
    public void execute_targetDisappeared_consumesPendingWithoutChange() {
        model.setPendingDeletion(sampleAlex.getEmail());
        model.deletePerson(sampleAlex);
        assertCommandFailure(confirm("e9000001@u.nus.edu"), model,
                "The pending student no longer exists. No data was changed.");
        assertTrue(model.getPendingDeletion().isEmpty());
    }

    @Test
    public void execute_repeatedConfirmation_secondAttemptFindsNoPendingDeletion() throws Exception {
        model.setPendingDeletion(sampleAlex.getEmail());
        confirm("e9000001@u.nus.edu").execute(model);
        assertCommandFailure(confirm("e9000001@u.nus.edu"), model,
                "There is no pending deletion. Use delete EMAIL first.");
        assertEquals(List.of(mei, realAlex), model.getAddressBook().getPersonList());
    }

    @Test
    public void failureMessages_requireFreshPreview() {
        String expected = "The student could not be deleted. No data was changed. Use delete EMAIL to start again.";
        assertEquals(expected, confirm("e9000001@u.nus.edu").getSaveFailureMessage("default"));
        assertEquals(expected, confirm("e9000001@u.nus.edu").getDisplayFailureMessage("default"));
    }

    @Test
    public void commandFlags_confirmationIsRecoveryGated() {
        assertTrue(confirm("e9000001@u.nus.edu").isDeletionConfirmation());
        assertFalse(confirm("e9000001@u.nus.edu").isReadOnly());
    }

    @Test
    public void equalsAndToString() {
        ConfirmDeleteCommand command = confirm("e9000001@u.nus.edu");
        assertTrue(command.equals(command));
        assertTrue(command.equals(confirm("E9000001@U.NUS.EDU")));
        assertEquals(command.hashCode(), confirm("E9000001@U.NUS.EDU").hashCode());
        assertFalse(command.equals(confirm("e9000002@u.nus.edu")));
        assertFalse(command.equals(null));
        assertEquals(ConfirmDeleteCommand.class.getCanonicalName() + "{email=e9000001@u.nus.edu}",
                command.toString());
    }

    private static ConfirmDeleteCommand confirm(String email) {
        return new ConfirmDeleteCommand(new Email(email));
    }

    private static Enrolment enrolment(String module, String semester) {
        return new Enrolment(new ModuleCode(module), new Semester(semester));
    }
}
