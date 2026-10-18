package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.EditEnrolCommand;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Section;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.enrolment.Team;
import seedu.address.model.person.Email;

public class EditEnrolCommandParserTest {
    private static final String SELECTORS = " /email e9000001@u.nus.edu /module CS2103T /semester AY26/27 S1";
    private final EditEnrolCommandParser parser = new EditEnrolCommandParser();

    @Test
    public void parse_normalisedSelectorsReorderedPrefixesAndDispatch() throws Exception {
        EditEnrolCommand expected = command(Optional.of(Optional.of(new Section("Lab 2"))),
                Optional.of(Optional.of(new Team("SEED-2"))));
        assertParseSuccess(parser, " /team SEED-2 /semester ay26/27\t s1 /section Lab\t 2 "
                + "/email E9000001@U.NUS.EDU /module cs2103t", expected);
        assertEquals(expected, new AddressBookParser().parseCommand("edit-enrol" + SELECTORS
                + " /section Lab 2 /team SEED-2"));
    }

    @Test
    public void parse_preserveClearAndLiteralClearAreDistinct() {
        assertParseSuccess(parser, SELECTORS + " /section clear", command(Optional.of(Optional.empty()),
                Optional.empty()));
        assertParseSuccess(parser, SELECTORS + " /team clear",
                command(Optional.empty(), Optional.of(Optional.empty())));
        assertParseSuccess(parser, SELECTORS + " /section Clear /team CLEAR",
                command(Optional.of(Optional.of(new Section("Clear"))), Optional.of(Optional.of(new Team("CLEAR")))));
        assertParseSuccess(parser, SELECTORS + " /section clear /team clear",
                command(Optional.of(Optional.empty()), Optional.of(Optional.empty())));
    }

    @Test
    public void parse_structurePrecedesValuesAndReportsLeftmostError() {
        syntax("text" + SELECTORS, "Unexpected text before the first parameter.");
        syntax(SELECTORS + " /unknown x /email x", "Unknown parameter: /unknown");
        syntax(SELECTORS + " /email x /unknown x", "Parameter /email can be supplied only once.");
        syntax(SELECTORS + " /27 x", "Invalid parameter syntax. "
                + "Use the parameter prefixes shown in the command format.");
        for (String prefix : new String[] {"email", "module", "semester", "section", "team"}) {
            String present = SELECTORS
                    + (prefix.equals("section") || prefix.equals("team") ? " /" + prefix + " x" : "");
            syntax(present + " /" + prefix + " x", "Parameter /" + prefix + " can be supplied only once.");
        }
    }

    @Test
    public void parse_missingAndInvalidValuesFollowSpecificationOrder() {
        syntax("", "Missing required parameter: /email");
        syntax(" /email e9000001@u.nus.edu", "Missing required parameter: /module");
        syntax(" /email e9000001@u.nus.edu /module CS2103T", "Missing required parameter: /semester");
        syntax(SELECTORS + " /team /section", "A value is required for /section.");
        syntax(SELECTORS + " /team", "A value is required for /team.");
        assertParseFailure(parser, " /team _ /email bad /module x", Email.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, " /email e9000001@u.nus.edu /module x", ModuleCode.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, SELECTORS.replace("AY26/27 S1", "AY26/28 S1") + " /section ---",
                Semester.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, SELECTORS, EditEnrolCommand.MESSAGE_NOT_EDITED);
    }

    @Test
    public void parse_invalidReplacementsRejectCompleteRequest() {
        for (String invalid : new String[] {"---", "T/12", "T_12", "x".repeat(31)}) {
            assertParseFailure(parser, SELECTORS + " /section " + invalid + " /team valid",
                    Section.MESSAGE_CONSTRAINTS);
            assertParseFailure(parser, SELECTORS + " /section valid /team " + invalid, Team.MESSAGE_CONSTRAINTS);
        }
        assertParseFailure(parser, SELECTORS + " /team X\n", Messages.MESSAGE_SINGLE_LINE);
        assertParseSuccess(parser, SELECTORS + " /team " + "x".repeat(30),
                command(Optional.empty(), Optional.of(Optional.of(new Team("x".repeat(30))))));
    }

    private void syntax(String input, String message) {
        assertParseFailure(parser, input, message + "\nUsage: " + EditEnrolCommand.MESSAGE_USAGE);
    }

    private EditEnrolCommand command(Optional<Optional<Section>> section, Optional<Optional<Team>> team) {
        return new EditEnrolCommand(new Email("e9000001@u.nus.edu"), new ModuleCode("CS2103T"),
                new Semester("AY26/27 S1"), section, team);
    }
}
