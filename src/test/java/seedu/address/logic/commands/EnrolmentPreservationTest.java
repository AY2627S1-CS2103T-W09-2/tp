package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.parser.AddressBookParser;
import seedu.address.model.ModelManager;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

class EnrolmentPreservationTest {
    @Test
    void editAndRemark_preserveEnrolmentsAndUnrelatedFields() throws Exception {
        Person person = new PersonBuilder().withEnrolments(
                new Enrolment(new ModuleCode("CS2103T"), new Semester("AY26/27 S1")),
                new Enrolment(new ModuleCode("CS2113T"), new Semester("AY26/27 S2"))).build();
        ModelManager model = new ModelManager();
        model.addPerson(person);
        AddressBookParser parser = new AddressBookParser();
        parser.parseCommand("edit 1 n/Changed Student p/123456 e/changed@u.nus.edu a/Changed Address t/test")
                .execute(model);
        Person edited = model.getAddressBook().getPersonList().get(0);
        assertEquals(person.getEnrolments(), edited.getEnrolments());
        parser.parseCommand("remark 1 r/Fictional test").execute(model);
        Person remarked = model.getAddressBook().getPersonList().get(0);
        assertEquals(edited.getEnrolments(), remarked.getEnrolments());
        assertEquals(edited.getName(), remarked.getName());
        assertEquals(edited.getEmail(), remarked.getEmail());
        assertEquals(edited.getTags(), remarked.getTags());
        assertEquals(List.of(person.getEnrolments().get(0), person.getEnrolments().get(1)),
                remarked.getEnrolments());
        parser.parseCommand("remark 1 r/").execute(model);
        assertEquals(person.getEnrolments(), model.getAddressBook().getPersonList().get(0).getEnrolments());
    }
}
