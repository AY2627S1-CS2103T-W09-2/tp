package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.EnrolCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Section;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.enrolment.Team;
import seedu.address.model.person.Email;

public class EnrolCommandParserTest {
    private static final String REQUIRED = " /email e9000001@u.nus.edu /module CS2103T /semester AY26/27 S1";
    private final EnrolCommandParser parser = new EnrolCommandParser();

    @Test
    public void parse_normalisesReorderedValuesAndAcademicYearSlash() throws Exception {
        EnrolCommand expected = command(Optional.of(new Section("Lab 2")), Optional.of(new Team("SEED-2")));
        assertParseSuccess(parser, "\t/team SEED-2 /semester ay26/27\t s1 /section Lab\t 2 "
                + "/module cs2103t /email E9000001@U.NUS.EDU\t", expected);
        assertEquals(expected, new AddressBookParser().parseCommand("enrol" + REQUIRED
                + " /section Lab 2 /team SEED-2"));
    }

    @Test
    public void parse_allAffiliationCombinations() {
        assertParseSuccess(parser, REQUIRED, command(Optional.empty(), Optional.empty()));
        assertParseSuccess(parser, REQUIRED + " /section T12",
                command(Optional.of(new Section("T12")), Optional.empty()));
        assertParseSuccess(parser, REQUIRED + " /team SEED",
                command(Optional.empty(), Optional.of(new Team("SEED"))));
        assertParseSuccess(parser, REQUIRED + " /section T12 /team SEED",
                command(Optional.of(new Section("T12")), Optional.of(new Team("SEED"))));
    }

    @Test
    public void parse_structuralErrorsHavePriorityAndUsage() {
        syntax("text" + REQUIRED, "Unexpected text before the first parameter.");
        syntax(REQUIRED + " /unknown x /email x", "Unknown parameter: /unknown");
        syntax(REQUIRED + " /email x /unknown x", "Parameter /email can be supplied only once.");
        syntax(REQUIRED + " /27 x", "Invalid parameter syntax. "
                + "Use the parameter prefixes shown in the command format.");
        for (String prefix : new String[] {"email", "module", "semester", "section", "team"}) {
            String present = REQUIRED + (prefix.equals("section") || prefix.equals("team") ? " /" + prefix + " x" : "");
            syntax(present + " /" + prefix + " x", "Parameter /" + prefix + " can be supplied only once.");
        }
    }

    @Test
    public void parse_missingAndBlankValuesFollowDocumentedOrder() {
        syntax("", "Missing required parameter: /email");
        syntax(" /email e9000001@u.nus.edu", "Missing required parameter: /module");
        syntax(" /email e9000001@u.nus.edu /module CS2103T", "Missing required parameter: /semester");
        for (String prefix : new String[] {"email", "module", "semester"}) {
            String blank = REQUIRED.replaceAll("/" + prefix + " [^/]*(?= /|$)", "/" + prefix + " ");
            if (prefix.equals("semester")) {
                blank = " /email e9000001@u.nus.edu /module CS2103T /semester\t";
            }
            syntax(blank, "A value is required for /" + prefix + ".");
        }
        syntax(REQUIRED + " /team /section", "A value is required for /section.");
        syntax(REQUIRED + " /team\t", "A value is required for /team.");
        assertParseFailure(parser, " /team _ /email bad /module x", Email.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, " /email e9000001@u.nus.edu /module x", ModuleCode.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_invalidFieldsRejectEntireCommand() {
        assertParseFailure(parser, REQUIRED.replace("CS2103T", "CS 2103T"), ModuleCode.MESSAGE_CONSTRAINTS);
        for (String invalid : new String[] {"AY26/28 S1", "AY26/27 S3", "AY26/27S1", "AY99/00 S1"}) {
            assertParseFailure(parser, REQUIRED.replace("AY26/27 S1", invalid), Semester.MESSAGE_CONSTRAINTS);
        }
        for (String invalid : new String[] {"---", "T/12", "T_12", "x".repeat(31)}) {
            assertParseFailure(parser, REQUIRED + " /section " + invalid, Section.MESSAGE_CONSTRAINTS);
            assertParseFailure(parser, REQUIRED + " /team " + invalid, Team.MESSAGE_CONSTRAINTS);
        }
        assertThrows(ParseException.class, () -> parser.parse(REQUIRED + "\n"));
    }

    private void syntax(String input, String message) {
        assertParseFailure(parser, input, message + "\nUsage: " + EnrolCommand.MESSAGE_USAGE);
    }

    private EnrolCommand command(Optional<Section> section, Optional<Team> team) {
        return new EnrolCommand(new Email("e9000001@u.nus.edu"),
                new Enrolment(new ModuleCode("CS2103T"), new Semester("AY26/27 S1"), section, team));
    }
}
