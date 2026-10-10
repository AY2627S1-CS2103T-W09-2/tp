package seedu.address.logic.parser;

import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.ConfirmDeleteCommand;
import seedu.address.model.person.Email;

/**
 * Checks the deletion confirmation's own messages. The shared argument boundaries are covered by
 * {@code ParserUtilTest#parseSoleEmail}.
 */
public class ConfirmDeleteCommandParserTest {
    private final ConfirmDeleteCommandParser parser = new ConfirmDeleteCommandParser();

    @Test
    public void parse_missingEmail_throwsConfirmUsage() {
        assertParseFailure(parser, " \t", "NUS email is required. Usage: confirm-delete EMAIL");
    }

    @Test
    public void parse_extraArgument_throwsConfirmUsageBeforeEmailValidation() {
        for (String args : new String[] {" e9000001@u.nus.edu extra", " yes please"}) {
            assertParseFailure(parser, args, "Confirm-delete accepts one NUS email only. Usage: confirm-delete EMAIL");
        }
    }

    @Test
    public void parse_genericAnswerOrIndex_rejectedWithEmailRule() {
        assertParseFailure(parser, " yes", Email.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, " 1", Email.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_paddedMixedCaseEmail_returnsCanonicalConfirmation() {
        assertParseSuccess(parser, " \tE9000001@U.NUS.EDU\t",
                new ConfirmDeleteCommand(new Email("e9000001@u.nus.edu")));
    }
}
