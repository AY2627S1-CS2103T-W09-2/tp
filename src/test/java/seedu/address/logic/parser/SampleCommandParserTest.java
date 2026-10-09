package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.logic.commands.ClearSamplesCommand;
import seedu.address.logic.commands.SampleCommand;
import seedu.address.logic.parser.exceptions.ParseException;

public class SampleCommandParserTest {
    private final AddressBookParser parser = new AddressBookParser();

    @Test
    public void parse_validLoadWhitespace_returnsSampleCommand() throws Exception {
        for (String input : new String[] {"sample load", " sample load ", "\tsample\tload\t",
            "sample     load", "sample\t\tload"}) {
            assertEquals(new SampleCommand(), parser.parseCommand(input));
        }
    }

    @Test
    public void parse_validClearWhitespace_returnsClearSamplesCommand() throws Exception {
        for (String input : new String[] {"sample clear", " sample clear ", "\tsample\tclear\t",
            "sample     clear", "sample\t\tclear"}) {
            assertEquals(new ClearSamplesCommand(), parser.parseCommand(input));
        }
    }

    @Test
    public void parse_loadWithExtraText_throwsLoadUsageMessage() {
        for (String input : new String[] {"sample load 5", "sample load extra", "sample\tload\t5"}) {
            ParseException error = assertThrows(ParseException.class, () -> parser.parseCommand(input));
            assertEquals(SampleCommand.MESSAGE_USAGE, error.getMessage());
        }
    }

    @Test
    public void parse_clearWithExtraText_throwsClearUsageMessage() {
        for (String input : new String[] {"sample clear 5", "sample clear extra", "sample\tclear\t5"}) {
            ParseException error = assertThrows(ParseException.class, () -> parser.parseCommand(input));
            assertEquals(ClearSamplesCommand.MESSAGE_USAGE, error.getMessage());
        }
    }

    @Test
    public void parse_missingUnknownOrWrongCaseSubcommand_throwsSubcommandUsageMessage() {
        for (String input : new String[] {"sample", "sample remove", "sample LOAD", "sample CLEAR"}) {
            ParseException error = assertThrows(ParseException.class, () -> parser.parseCommand(input));
            assertEquals(SampleCommand.MESSAGE_SUBCOMMAND_USAGE, error.getMessage());
        }
    }

    @Test
    public void parse_lineBreakOrControl_throwsSingleLineMessage() {
        for (String control : new String[] {"\n", "\r", "\0", "\u0085", "\u2028", "\u2029"}) {
            for (String subcommand : new String[] {"load", "clear"}) {
                ParseException error = assertThrows(ParseException.class, () ->
                        parser.parseCommand("sample " + subcommand + control));
                assertEquals(Messages.MESSAGE_SINGLE_LINE, error.getMessage());
            }
        }
    }
}
