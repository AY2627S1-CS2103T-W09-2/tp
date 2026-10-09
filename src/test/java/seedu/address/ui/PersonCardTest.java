package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Section;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.enrolment.Team;
import seedu.address.model.person.GitHub;
import seedu.address.model.person.Telegram;

public class PersonCardTest {
    @Test
    public void contactText_presentContact_showsSavedValue() {
        assertEquals("Telegram: Alex_Tan",
                PersonCard.contactText(PersonCard.TELEGRAM_LABEL, Optional.of(new Telegram("@Alex_Tan"))));
        assertEquals("GitHub: clear",
                PersonCard.contactText(PersonCard.GITHUB_LABEL, Optional.of(new GitHub("clear"))));
    }

    @Test
    public void contactText_absentContact_showsNotProvided() {
        assertEquals("Telegram: Not provided", PersonCard.contactText(PersonCard.TELEGRAM_LABEL, Optional.empty()));
        assertEquals("GitHub: Not provided", PersonCard.contactText(PersonCard.GITHUB_LABEL, Optional.empty()));
    }

    @Test
    public void enrolmentLine_presentAndAbsentAffiliations_showsValueOrNotAssigned() {
        assertEquals("CS2103T | AY26/27 S1 | Section: T12 | Team: SEED",
                PersonCard.enrolmentLine(enrolment("CS2103T", "AY26/27 S1", "T12", "SEED")));
        assertEquals("CS2113T | AY26/27 S2 | Section: T14 | Team: Not assigned",
                PersonCard.enrolmentLine(enrolment("CS2113T", "AY26/27 S2", "T14", null)));
        assertEquals("CS2103T | AY26/27 S1 | Section: Not assigned | Team: Not assigned",
                PersonCard.enrolmentLine(enrolment("CS2103T", "AY26/27 S1", null, null)));
    }

    @Test
    public void enrolmentText_noEnrolments_showsNone() {
        assertEquals("Enrolments: none", PersonCard.enrolmentText(List.of()));
    }

    @Test
    public void enrolmentText_unsortedEnrolments_showsCountThenEveryLineInDisplayOrder() {
        List<Enrolment> enrolments = List.of(enrolment("CS2113T", "AY26/27 S2", "T14", null),
                enrolment("CS2103T", "AY26/27 S1", "T12", "SEED"));
        assertEquals("Enrolments: 2\n"
                + "CS2103T | AY26/27 S1 | Section: T12 | Team: SEED\n"
                + "CS2113T | AY26/27 S2 | Section: T14 | Team: Not assigned",
                PersonCard.enrolmentText(enrolments));
    }

    static Enrolment enrolment(String module, String semester, String section, String team) {
        return new Enrolment(new ModuleCode(module), new Semester(semester),
                Optional.ofNullable(section).map(Section::new), Optional.ofNullable(team).map(Team::new));
    }
}
