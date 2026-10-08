package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.SampleCommand;
import seedu.address.logic.parser.exceptions.ParseException;

public class SampleCommandParserTest {
    private final AddressBookParser parser = new AddressBookParser();

    @Test
    public void parse_validWhitespace_returnsSampleCommand() throws Exception {
        for (String input : new String[] {"sample load", " sample load ", "\tsample\tload\t",
            "sample     load", "sample\t\tload"}) {
            assertEquals(new SampleCommand(), parser.parseCommand(input));
        }
    }

    @Test
    public void parse_missingOrExtraText_throwsExactUsageMessage() {
        for (String input : new String[] {"sample", "sample load 5", "sample load extra", "sample clear",
            "sample LOAD"}) {
            ParseException error = assertThrows(ParseException.class, () -> parser.parseCommand(input));
            assertEquals(SampleCommand.MESSAGE_USAGE, error.getMessage());
        }
    }

    @Test
    public void parse_lineBreakOrControl_throwsSingleLineMessage() {
        for (String control : new String[] {"\n", "\r", "\0", "\u0085", "\u2028", "\u2029"}) {
            ParseException error = assertThrows(ParseException.class, () ->
                    parser.parseCommand("sample load" + control));
            assertEquals(Messages.MESSAGE_SINGLE_LINE, error.getMessage());
        }
    }
}
