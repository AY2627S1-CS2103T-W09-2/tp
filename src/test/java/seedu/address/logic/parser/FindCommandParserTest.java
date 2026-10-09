package seedu.address.logic.parser;

import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.FindCommand;

public class FindCommandParserTest {
    private final FindCommandParser parser = new FindCommandParser();

    @Test
    public void parse_emptyQuery_rejected() {
        for (String query : new String[] {"", " ", " \t \t"}) {
            assertParseFailure(parser, query, FindCommand.MESSAGE_EMPTY_QUERY + "\n" + FindCommand.MESSAGE_USAGE);
        }
    }

    @Test
    public void parse_whitespace_preservesWholeQuery() {
        assertParseSuccess(parser, " \tAlex Tan\t ", new FindCommand("Alex Tan"));
        assertParseSuccess(parser, "Alex  Tan", new FindCommand("Alex  Tan"));
        assertParseSuccess(parser, "Alex\t\tTan", new FindCommand("Alex  Tan"));
        assertParseSuccess(parser, "Alex\tTan", new FindCommand("Alex Tan"));
        assertParseSuccess(parser, "/name Alex * .+", new FindCommand("/name Alex * .+"));
        assertParseSuccess(parser, "Élodie", new FindCommand("Élodie"));
        assertParseSuccess(parser, " \t@Mixed_Handle ", new FindCommand("@Mixed_Handle"));
        assertParseSuccess(parser, "@", new FindCommand("@"));
        assertParseSuccess(parser, "@@handle", new FindCommand("@@handle"));
    }

    @Test
    public void parse_length_countsCodePointsAfterTrimming() {
        assertParseSuccess(parser, "x", new FindCommand("x"));
        assertParseSuccess(parser, " \t" + "x".repeat(100) + " ", new FindCommand("x".repeat(100)));
        String supplementary = new String(Character.toChars(0x1f600));
        assertParseSuccess(parser, supplementary.repeat(100), new FindCommand(supplementary.repeat(100)));
        assertParseFailure(parser, supplementary.repeat(101), FindCommand.MESSAGE_LONG_QUERY);
        assertParseFailure(parser, "x".repeat(101), FindCommand.MESSAGE_LONG_QUERY);
    }

    @Test
    public void parse_controlsAndLineBreaks_rejectedBeforeTrimming() {
        for (String control : new String[] {"\n", "\r", "\r\n", "\0", "\u000b", "\u007f", "\u0085",
            "\u2028", "\u2029"}) {
            assertParseFailure(parser, control + "Alex", Messages.MESSAGE_SINGLE_LINE);
            assertParseFailure(parser, "Alex" + control, Messages.MESSAGE_SINGLE_LINE);
            assertParseFailure(parser, "Alex" + control + "Tan", Messages.MESSAGE_SINGLE_LINE);
        }
    }
}
