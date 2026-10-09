package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.ui.PersonCardTest.enrolment;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.Person;
import seedu.address.model.util.SampleDataUtil;
import seedu.address.testutil.PersonBuilder;

public class PersonDetailsPanelTest {
    @Test
    public void profileLines_sampleWithEnrolments_showsCompleteProfile() {
        Person alex = SampleDataUtil.getSamplePersons()[0];
        assertEquals(List.of("Fictional sample", "NUS email: e9000001@u.nus.edu", "Telegram: socdex_demo_alex",
                "GitHub: socdex-demo-alex", "Enrolments: 2",
                "CS2103T | AY26/27 S1 | Section: T12 | Team: SEED",
                "CS2113T | AY26/27 S2 | Section: T14 | Team: Not assigned"),
                PersonDetailsPanel.profileLines(alex));
    }

    @Test
    public void profileLines_noContactsOrEnrolments_showsDisplayOnlyLabels() {
        Person nur = new PersonBuilder().withName("Nur Aisyah").withEmail("e9000005@u.nus.edu").withoutContacts()
                .withEnrolments().withSample(true).build();
        assertEquals(List.of("Fictional sample", "NUS email: e9000005@u.nus.edu", "Telegram: Not provided",
                "GitHub: Not provided", "Enrolments: none"), PersonDetailsPanel.profileLines(nur));
    }

    @Test
    public void profileLines_nonSample_omitsSampleLabel() {
        Person ravi = new PersonBuilder().withName("Ravi Kumar").withEmail("e9000004@u.nus.edu")
                .withTelegram("ravi_k").withGitHub("ravi-k")
                .withEnrolments(enrolment("CS2103T", "AY26/27 S1", null, null)).build();
        assertEquals(List.of("NUS email: e9000004@u.nus.edu", "Telegram: ravi_k", "GitHub: ravi-k", "Enrolments: 1",
                "CS2103T | AY26/27 S1 | Section: Not assigned | Team: Not assigned"),
                PersonDetailsPanel.profileLines(ravi));
    }
}
