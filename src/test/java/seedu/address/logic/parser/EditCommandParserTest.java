package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.EditCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Email;
import seedu.address.model.person.GitHub;
import seedu.address.model.person.Telegram;

public class EditCommandParserTest {
    private static final String TARGET = " /email alex@u.nus.edu";
    private final EditCommandParser parser = new EditCommandParser();

    @Test
    public void parse_clearAndLiteralClear_areDistinct() throws Exception {
        ModelManager model = new ModelManager();
        new AddCommandParser().parse(" /name Alex" + TARGET).execute(model);
        parser.parse(TARGET + " /telegram @clear /github Clear").execute(model);
        assertEquals("clear", model.getFilteredPersonList().get(0).getTelegram().orElseThrow().value);
        assertEquals("Clear", model.getFilteredPersonList().get(0).getGitHub().orElseThrow().value);
        parser.parse(" /telegram clear /email ALEX@U.NUS.EDU").execute(model);
        assertEquals(true, model.getFilteredPersonList().get(0).getTelegram().isEmpty());
        assertEquals("Clear", model.getFilteredPersonList().get(0).getGitHub().orElseThrow().value);
    }

    @Test
    public void parse_invalidStructureAndRetiredSyntax_rejected() {
        syntax("1 e/alex@u.nus.edu", "Unexpected text before the first parameter.");
        syntax(" /telegram clear", "Missing required parameter: /email");
        syntax(TARGET + " /email other@u.nus.edu", "Parameter /email can be supplied only once.");
        for (String prefix : new String[] {"/name", "/new-email", "/section", "/team"}) {
            syntax(TARGET + " " + prefix + " value", "Unknown parameter: " + prefix);
        }
        syntax(TARGET + " /github", "A value is required for /github.");
        syntax(TARGET + " /telegram /github Clear", "A value is required for /telegram.");
        assertParseFailure(parser, TARGET, EditCommand.MESSAGE_NOT_EDITED);
        assertThrows(ParseException.class, () -> parser.parse(TARGET + " /github a\n"));
    }

    @Test
    public void parse_invalidReplacement_validatedBeforeTargetLookup() {
        assertParseFailure(parser, " /email bad /telegram invalid", Email.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, TARGET + " /telegram @@@clear /github bad_", Telegram.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, TARGET + " /github bad_", GitHub.MESSAGE_CONSTRAINTS);
    }

    private void syntax(String input, String message) {
        assertParseFailure(parser, input, message + "\nUsage: " + EditCommand.MESSAGE_USAGE);
    }
}
