package seedu.address.logic.parser;

import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.ViewCommand;
import seedu.address.model.person.Email;

public class ViewCommandParserTest {
    private final ViewCommandParser parser = new ViewCommandParser();

    @Test
    public void parse_missingEmail_rejected() {
        for (String args : new String[] {"", " ", " \t \t"}) {
            assertParseFailure(parser, args, "NUS email is required. Usage: view EMAIL");
        }
    }

    @Test
    public void parse_extraTokens_rejectedBeforeEmailValidation() {
        for (String args : new String[] {" e9000001@u.nus.edu e9000002@u.nus.edu", " e9000001@u.nus.edu\textra",
            " /email e9000001@u.nus.edu", " not-an-email also-bad", " 1 2"}) {
            assertParseFailure(parser, args, "View accepts one NUS email only. Usage: view EMAIL");
        }
    }

    @Test
    public void parse_invalidSingleToken_showsEmailRule() {
        for (String args : new String[] {" Alex", " 1", " e9000001@gmail.com", " /email", " a..b@u.nus.edu",
            " @socdex_demo_alex"}) {
            assertParseFailure(parser, args, Email.MESSAGE_CONSTRAINTS);
        }
    }

    @Test
    public void parse_lineBreak_rejected() {
        assertParseFailure(parser, " e9000001@u.nus.edu\n", Messages.MESSAGE_SINGLE_LINE);
    }

    @Test
    public void parse_validEmail_normalisedToCanonical() {
        ViewCommand expected = new ViewCommand(new Email("e9000001@u.nus.edu"));
        assertParseSuccess(parser, " e9000001@u.nus.edu", expected);
        assertParseSuccess(parser, " \tE9000001@U.NUS.EDU\t ", expected);
    }
}
