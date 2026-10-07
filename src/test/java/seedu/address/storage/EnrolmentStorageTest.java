package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.commons.util.JsonUtil;
import seedu.address.model.AddressBook;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Section;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.enrolment.Team;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

class EnrolmentStorageTest {
    @TempDir
    private Path folder;

    @Test
    void saveReload_preservesEveryFieldAndOptionalAbsenceAcrossOwners() throws Exception {
        Enrolment assigned = new Enrolment(new ModuleCode("CS2103T"), new Semester("AY26/27 S1"),
                Optional.of(new Section("Lab 2")), Optional.of(new Team("SEED-2")));
        Enrolment absent = new Enrolment(new ModuleCode("CS2113T"), new Semester("AY26/27 S2"));
        Enrolment sectionOnly = new Enrolment(new ModuleCode("MA1521"), new Semester("AY26/27 S1"),
                Optional.of(new Section("T12")), Optional.empty());
        Enrolment teamOnly = new Enrolment(new ModuleCode("CS2103T"), new Semester("AY25/26 S1"),
                Optional.empty(), Optional.of(new Team("Team 4")));
        Person first = new PersonBuilder().withEmail("e9000001@u.nus.edu")
                .withEnrolments(assigned, absent, sectionOnly, teamOnly).build();
        Person second = new PersonBuilder(first).withName("Another Student").withEmail("e9000002@u.nus.edu").build();
        AddressBook roster = new AddressBook();
        roster.addPerson(first);
        roster.addPerson(second);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(folder.resolve("roster.json"));
        storage.saveAddressBook(roster);
        assertEquals(roster, storage.readAddressBook().orElseThrow());
        assertEquals(first, new JsonAdaptedPerson(first).toModelType());
        assertTrue(Files.readString(storage.getAddressBookFilePath()).contains("\"module\" : \"CS2103T\""));
    }

    @Test
    void loadLegacyWithoutEnrolments_isEmptyAndDoesNotRewriteFile() throws Exception {
        Person person = new PersonBuilder().withEmail("e9000001@u.nus.edu").build();
        String json = JsonUtil.toJsonString(new JsonAdaptedPerson(person));
        json = json.replaceAll(",?\\s*\"enrolments\"\\s*:\\s*\\[\\s*\\]", "");
        Path target = folder.resolve("legacy.json");
        Files.writeString(target, "{\"persons\":[" + json + "]}");
        byte[] original = Files.readAllBytes(target);
        Person loaded = new JsonAddressBookStorage(target).readAddressBook().orElseThrow().getPersonList().get(0);
        assertEquals(person, loaded);
        assertTrue(loaded.getEnrolments().isEmpty());
        assertArrayEquals(original, Files.readAllBytes(target));
    }

    @Test
    void malformedEnrolments_rejectWholeFileAndPreserveBytes() throws Exception {
        String validPerson = JsonUtil.toJsonString(new JsonAdaptedPerson(new PersonBuilder().build()));
        String context = "{\"module\":\"CS2103T\",\"semester\":\"AY26/27 S1\"}";
        String assigned = "{\"module\":\"CS2103T\",\"semester\":\"AY26/27 S1\",\"section\":\"T14\"}";
        for (String invalid : List.of("null", "[null]", "[{}]", "[" + context + "," + assigned + "]",
                "[" + context.replace("CS2103T", "CS210") + "]",
                "[" + context.replace("AY26/27", "AY26/28") + "]",
                "[" + context.replace("CS2103T", "cs2103t") + "]",
                "[" + context.replace("S1", "S3") + "]",
                "[" + assigned.replace("T14", "") + "]",
                "[" + assigned.replace("T14", "T/14") + "]",
                "[" + assigned.replace("T14", " T14 ") + "]",
                "[" + assigned.replace("\"T14\"", "123") + "]", "{}", "123")) {
            String invalidPerson = validPerson.replaceAll("\"enrolments\"\\s*:\\s*\\[\\s*\\]",
                    "\"enrolments\":" + invalid);
            Path target = folder.resolve("invalid.json");
            Files.writeString(target, "{\"persons\":[" + validPerson + "," + invalidPerson + "]}");
            byte[] original = Files.readAllBytes(target);
            assertThrows(DataLoadingException.class, () -> new JsonAddressBookStorage(target).readAddressBook());
            assertArrayEquals(original, Files.readAllBytes(target));
        }
    }

    @Test
    void adapter_rejectsInvalidTeamAndAcceptsExplicitNullAffiliations() throws Exception {
        JsonAdaptedEnrolment absent = new JsonAdaptedEnrolment("CS2103T", "AY26/27 S1", null, null);
        assertTrue(absent.toModelType().getSection().isEmpty());
        assertTrue(absent.toModelType().getTeam().isEmpty());
        assertThrows(IllegalValueException.class, () ->
                new JsonAdaptedEnrolment("CS2103T", "AY26/27 S1", null, "---").toModelType());
        assertThrows(IllegalValueException.class, () ->
                new JsonAdaptedEnrolment("CS2103T", "AY26/27 S1", null, " Team 4 ").toModelType());
    }
}
