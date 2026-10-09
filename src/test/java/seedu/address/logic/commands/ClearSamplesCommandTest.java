package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonOrder;
import seedu.address.model.util.SampleDataUtil;
import seedu.address.testutil.PersonBuilder;

public class ClearSamplesCommandTest {

    @Test
    public void execute_fixedFixture_removesFiveProfilesAndFiveEnrolments() {
        ModelManager model = new ModelManager(SampleDataUtil.getSampleAddressBook(), new UserPrefs());

        CommandResult result = new ClearSamplesCommand().execute(model);

        assertEquals(String.format(ClearSamplesCommand.MESSAGE_SUCCESS, 5, 5), result.getFeedbackToUser());
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
        assertTrue(model.getFilteredPersonList().isEmpty());
    }

    @Test
    public void execute_hiddenEditedSamples_removesOnlyClassifiedRecordsAndShowsFullSortedRoster() {
        ModelManager model = new ModelManager(SampleDataUtil.getSampleAddressBook(), new UserPrefs());
        Person hiddenSample = model.getAddressBook().getPersonList().stream()
                .filter(person -> person.getName().fullName.equals("Nur Aisyah"))
                .findFirst()
                .orElseThrow();
        Person editedSample = new PersonBuilder(hiddenSample)
                .withName("Changed Person")
                .withEmail("changed@u.nus.edu")
                .withEnrolments(enrolment("CS2103T", "AY26/27 S1"), enrolment("CS2100", "AY26/27 S2"))
                .build();
        model.setPerson(hiddenSample, editedSample);
        Person realAlex = new PersonBuilder()
                .withName("Alex Tan")
                .withEmail("real.alex@u.nus.edu")
                .withTelegram("socdex_demo_alex")
                .withGitHub("socdex-demo-alex")
                .withEnrolments(enrolment("CS2103T", "AY26/27 S1"))
                .build();
        Person realZoe = new PersonBuilder().withName("Zoe Lim").withEmail("zoe@u.nus.edu").build();
        model.addPerson(realZoe);
        model.addPerson(realAlex);
        model.updateFilteredPersonList(person -> !person.isSample() && person.getName().fullName.startsWith("Zoe"));

        CommandResult result = new ClearSamplesCommand().execute(model);

        assertEquals(String.format(ClearSamplesCommand.MESSAGE_SUCCESS, 5, 7), result.getFeedbackToUser());
        assertTrue(result.isUpdateSelection());
        assertTrue(result.isPreserveSurvivingSelection());
        assertEquals(List.of(realAlex, realZoe), model.getAddressBook().getPersonList().stream()
                .sorted(PersonOrder.BY_NAME_THEN_EMAIL).toList());
        assertEquals(List.of(realAlex, realZoe), model.getFilteredPersonList());
        assertTrue(model.getAddressBook().getPersonList().stream().noneMatch(Person::isSample));
    }

    @Test
    public void execute_noSamples_isNoOpAndPreservesFilter() {
        ModelManager model = new ModelManager();
        Person alice = new PersonBuilder().withName("Alice Lim").withEmail("alice@u.nus.edu").build();
        Person bob = new PersonBuilder().withName("Bob Tan").withEmail("bob@u.nus.edu").build();
        model.addPerson(alice);
        model.addPerson(bob);
        model.updateFilteredPersonList(person -> person.equals(bob), PersonOrder.BY_NAME_THEN_EMAIL.reversed());

        CommandResult result = new ClearSamplesCommand().execute(model);

        assertEquals(ClearSamplesCommand.MESSAGE_NO_SAMPLES, result.getFeedbackToUser());
        assertFalse(result.isUpdateSelection());
        assertEquals(List.of(alice, bob), model.getAddressBook().getPersonList());
        assertEquals(List.of(bob), model.getFilteredPersonList());
    }

    @Test
    public void equals_statelessCommands_equal() {
        assertEquals(new ClearSamplesCommand(), new ClearSamplesCommand());
        assertEquals(ClearSamplesCommand.MESSAGE_SAVE_FAILURE,
                new ClearSamplesCommand().getSaveFailureMessage("Default failure"));
    }

    private static Enrolment enrolment(String module, String semester) {
        return new Enrolment(new ModuleCode(module), new Semester(semester));
    }
}
