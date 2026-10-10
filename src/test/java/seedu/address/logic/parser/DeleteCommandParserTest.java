package seedu.address.logic.parser;

import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.DeleteCommand;
import seedu.address.model.person.Email;

/**
 * Checks the deletion preview's own messages. The shared argument boundaries are covered by
 * {@code ParserUtilTest#parseSoleEmail}.
 */
public class DeleteCommandParserTest {
    private final DeleteCommandParser parser = new DeleteCommandParser();

    @Test
    public void parse_missingEmail_throwsDeleteUsage() {
        assertParseFailure(parser, " \t", "NUS email is required. Usage: delete EMAIL");
    }

    @Test
    public void parse_extraArgument_throwsDeleteUsageBeforeEmailValidation() {
        for (String args : new String[] {" e9000001@u.nus.edu extra", " 1 2"}) {
            assertParseFailure(parser, args, "Delete accepts one NUS email only. Usage: delete EMAIL");
        }
    }

    @Test
    public void parse_index_rejectedWithEmailRule() {
        assertParseFailure(parser, " 1", Email.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_paddedMixedCaseEmail_returnsCanonicalPreview() {
        assertParseSuccess(parser, " \tE9000001@U.NUS.EDU\t", new DeleteCommand(new Email("e9000001@u.nus.edu")));
    }
}
