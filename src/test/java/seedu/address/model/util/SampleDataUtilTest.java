package seedu.address.model.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.person.Person;

public class SampleDataUtilTest {

    @Test
    public void getSamplePersons_returnsExactFixedFixture() {
        List<Person> profiles = List.of(SampleDataUtil.getSamplePersons());

        assertEquals(List.of("Alex Tan", "Alex Tan", "Mei Lim", "Nur Aisyah", "Ravi Kumar"),
                profiles.stream().map(person -> person.getName().fullName).toList());
        assertEquals(List.of("e9000001@u.nus.edu", "e9000002@u.nus.edu", "e9000003@u.nus.edu",
                "e9000005@u.nus.edu", "e9000004@u.nus.edu"),
                profiles.stream().map(person -> person.getEmail().value).toList());
        assertTrue(profiles.stream().allMatch(Person::isSample));
        assertEquals(Arrays.asList("socdex_demo_alex", null, "socdex_demo_mei", null, "socdex_demo_ravi"),
                profiles.stream().map(person -> person.getTelegram().map(Object::toString).orElse(null)).toList());
        assertEquals(Arrays.asList("socdex-demo-alex", "socdex-demo-alex2", null, null, "socdex-demo-ravi"),
                profiles.stream().map(person -> person.getGitHub().map(Object::toString).orElse(null)).toList());
        assertEquals(List.of(2, 1, 1, 0, 1),
                profiles.stream().map(person -> person.getEnrolments().size()).toList());

        List<Enrolment> enrolments = profiles.stream().flatMap(person -> person.getEnrolments().stream()).toList();
        assertEquals(5, enrolments.size());
        assertEnrolment(enrolments.get(0), "CS2103T", "AY26/27 S1", "T12", "SEED");
        assertEnrolment(enrolments.get(1), "CS2113T", "AY26/27 S2", "T14", null);
        assertEnrolment(enrolments.get(2), "CS2103T", "AY26/27 S1", "T14", "SPROUT");
        assertEnrolment(enrolments.get(3), "CS2103T", "AY26/27 S1", "T12", "SEED");
        assertEnrolment(enrolments.get(4), "CS2103T", "AY26/27 S1", null, null);
    }

    private static void assertEnrolment(Enrolment enrolment, String module, String semester,
            String section, String team) {
        assertEquals(module, enrolment.getModuleCode().value);
        assertEquals(semester, enrolment.getSemester().value);
        assertEquals(section, enrolment.getSection().map(Object::toString).orElse(null));
        assertEquals(team, enrolment.getTeam().map(Object::toString).orElse(null));
    }
}
