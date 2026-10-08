package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.logic.parser.AddressBookParser;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.person.GitHub;
import seedu.address.model.person.Person;
import seedu.address.model.person.Telegram;
import seedu.address.testutil.PersonBuilder;

/**
 * Tests that inherited commands and copy paths preserve contacts and the sample classification.
 */
class ProfileFieldPreservationTest {
    private static final Optional<Telegram> TELEGRAM = Optional.of(new Telegram("Alex_Demo"));
    private static final Optional<GitHub> GITHUB = Optional.of(new GitHub("AlexDemo"));

    private final Person original = new PersonBuilder().withName("Alex Demo").withEmail("e9000001@u.nus.edu")
            .withTelegram("Alex_Demo").withGitHub("AlexDemo").withSample(true).withRemark("Fictional remark")
            .withTags("demo").withEnrolments(new Enrolment(new ModuleCode("CS2103T"), new Semester("AY26/27 S1")))
            .build();
    private final AddressBookParser parser = new AddressBookParser();

    @Test
    void edit_noOp_preservesAllFields() throws Exception {
        Model model = modelWith(original);
        parser.parseCommand("edit /email e9000001@u.nus.edu /telegram @Alex_Demo").execute(model);

        Person edited = model.getAddressBook().getPersonList().get(0);
        assertContactsAndSamplePreserved(edited);
        assertEquals(original, edited);
    }

    @Test
    void remark_preservesContactsAndSampleClassification() throws Exception {
        Model model = modelWith(original);
        parser.parseCommand("remark 1 r/Changed remark").execute(model);

        Person remarked = model.getAddressBook().getPersonList().get(0);
        assertContactsAndSamplePreserved(remarked);
        assertEquals(new PersonBuilder(original).withRemark("Changed remark").build(), remarked);
    }

    @Test
    void withEnrolments_preservesContactsAndSampleClassification() {
        Person updated = original.withEnrolments(List.of());

        assertContactsAndSamplePreserved(updated);
        assertTrue(updated.getEnrolments().isEmpty());
        assertEquals(new PersonBuilder(original).withEnrolments().build(), updated);
    }

    @Test
    void add_createsNonSampleProfileWithoutContacts() throws Exception {
        Model model = new ModelManager();
        parser.parseCommand("student add /name Mei Tan /email e9000002@u.nus.edu").execute(model);

        Person added = model.getAddressBook().getPersonList().get(0);
        assertFalse(added.isSample());
        assertTrue(added.getTelegram().isEmpty());
        assertTrue(added.getGitHub().isEmpty());
        assertTrue(added.getEnrolments().isEmpty());
    }

    private static Model modelWith(Person person) {
        Model model = new ModelManager();
        model.addPerson(person);
        return model;
    }

    /**
     * Asserts the stored contacts, sample classification and tags without relying on {@code PersonBuilder} copying.
     */
    private void assertContactsAndSamplePreserved(Person person) {
        assertEquals(TELEGRAM, person.getTelegram());
        assertEquals(GITHUB, person.getGitHub());
        assertTrue(person.isSample());
        assertEquals(original.getTags(), person.getTags());
        assertEquals(original.getEmail(), person.getEmail());
    }
}
