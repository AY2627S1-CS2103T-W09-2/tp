package seedu.address.model.enrolment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

class EnrolmentTest {
    @Test
    void module_validationAndCanonicalisation() {
        assertEquals("CS2103T", new ModuleCode(" \tcs2103t\t ").value);
        for (String valid : List.of("MA1521", "ABCD1234ABC", "cs2113t")) {
            assertTrue(ModuleCode.isValidModuleCode(valid));
        }
        for (String invalid : List.of("", "CS210", "C2103", "ABCDE1234", "CS2103ABCD", "CS 2103T",
                "CS2103\n", "ſſ2103", "ＣＳ2103", "CS٢١٠٣")) {
            assertFalse(ModuleCode.isValidModuleCode(invalid), invalid);
            assertThrows(IllegalArgumentException.class, () -> new ModuleCode(invalid));
        }
        assertThrows(NullPointerException.class, () -> new ModuleCode(null));
    }

    @Test
    void semester_validationAndOrdering() {
        assertEquals("AY26/27 S1", new Semester(" \tay26/27 \t s1\t").value);
        for (String valid : List.of("AY00/01 S1", "AY98/99 S2", "ay26/27 s2")) {
            assertTrue(Semester.isValidSemester(valid));
        }
        for (String invalid : List.of("", "AY99/00 S1", "AY26/28 S1", "AY26/27 S3", "AY26/27S1",
                "AY26/27 ST1", "AY26/27\nS1", "AY26/27 ſ1", "AY2026/2027 S1")) {
            assertFalse(Semester.isValidSemester(invalid), invalid);
            assertThrows(IllegalArgumentException.class, () -> new Semester(invalid));
        }
        assertEquals(2026, new Semester("AY26/27 S1").getStartYear());
        assertEquals(2, new Semester("AY26/27 S2").getSemesterNumber());
        Enrolment early = context("MA1521", "AY25/26 S2");
        Enrolment next = context("CS2103T", "AY26/27 S1");
        Enrolment sameSemester = context("MA1521", "AY26/27 S1");
        Enrolment late = context("CS2103T", "AY26/27 S2");
        assertEquals(List.of(early, next, sameSemester, late),
                List.of(late, sameSemester, next, early).stream().sorted(Enrolment.DISPLAY_ORDER).toList());
    }

    @Test
    void affiliations_validateBoundariesAndPreserveCase() {
        assertEquals("Lab 2", new Section(" \tLab \t 2 ").value);
        assertEquals("Team Four", new Team(" Team  Four ").value);
        for (String valid : List.of("T", "SEED-2", "A".repeat(30), "- 2 -")) {
            assertTrue(Section.isValidSection(valid));
            assertTrue(Team.isValidTeam(valid));
        }
        for (String invalid : List.of("", " \t", "---", " - - ", "T/12", "SEED_2", "A".repeat(31),
                "équipe", "T12\n", "T12\r", "T12\u0000")) {
            assertFalse(Section.isValidSection(invalid), invalid);
            assertFalse(Team.isValidTeam(invalid), invalid);
            assertThrows(IllegalArgumentException.class, () -> new Section(invalid));
            assertThrows(IllegalArgumentException.class, () -> new Team(invalid));
        }
        assertNotEquals(new Section("Lab"), new Section("lab"));
        assertNotEquals(new Team("Seed"), new Team("SEED"));
    }

    @Test
    void enrolment_keyExcludesAffiliationsButEqualityIncludesThem() {
        Enrolment absent = context("CS2103T", "AY26/27 S1");
        Enrolment assigned = new Enrolment(new ModuleCode("cs2103t"), new Semester("ay26/27 s1"),
                Optional.of(new Section("T12")), Optional.of(new Team("SEED")));
        assertTrue(absent.hasSameKey(assigned));
        assertNotEquals(absent, assigned);
        assertFalse(absent.hasSameKey(context("CS2103T", "AY26/27 S2")));
        assertFalse(absent.hasSameKey(context("CS2113T", "AY26/27 S1")));
        assertTrue(absent.getSection().isEmpty());
        assertTrue(absent.getTeam().isEmpty());
        assertEquals(absent.hashCode(), context("cs2103t", "ay26/27 s1").hashCode());
        assertThrows(NullPointerException.class, () ->
                new Enrolment(new ModuleCode("CS2103T"), new Semester("AY26/27 S1"), null, Optional.empty()));
    }

    @Test
    void person_defensiveCopiesUniqueKeysAndFullEquality() {
        Enrolment first = context("CS2103T", "AY26/27 S1");
        Enrolment second = context("CS2113T", "AY26/27 S1");
        Person original = new PersonBuilder().build();
        List<Enrolment> values = new ArrayList<>(List.of(first));
        Person enrolled = original.withEnrolments(values);
        values.add(second);
        assertEquals(List.of(first), enrolled.getEnrolments());
        assertThrows(UnsupportedOperationException.class, () -> enrolled.getEnrolments().add(second));
        assertNotEquals(original, enrolled);
        assertTrue(original.isSamePerson(enrolled));
        Person copy = new PersonBuilder(enrolled).build();
        assertEquals(enrolled, copy);
        assertEquals(enrolled.hashCode(), copy.hashCode());
        assertEquals(enrolled.getEnrolments(), new PersonBuilder(enrolled).withName("Other Student")
                .withEmail("other@u.nus.edu").build().getEnrolments());
        assertEquals(List.of(first), new PersonBuilder().withName("Other Student").withEnrolments(first)
                .build().getEnrolments());
        Enrolment duplicate = new Enrolment(new ModuleCode("cs2103t"), new Semester("ay26/27 s1"),
                Optional.of(new Section("T14")), Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> original.withEnrolments(List.of(first, duplicate)));
        assertEquals(List.of(first, second), original.withEnrolments(List.of(first, second)).getEnrolments());
        assertTrue(enrolled.withEnrolments(List.of()).getEnrolments().isEmpty());
    }

    private Enrolment context(String module, String semester) {
        return new Enrolment(new ModuleCode(module), new Semester(semester));
    }
}
