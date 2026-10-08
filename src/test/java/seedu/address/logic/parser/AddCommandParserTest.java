package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Email;
import seedu.address.model.person.GitHub;
import seedu.address.model.person.Name;
import seedu.address.model.person.Telegram;

public class AddCommandParserTest {
    private static final String VALID = " /name Alex Tan /email alex@u.nus.edu";
    private final AddCommandParser parser = new AddCommandParser();

    @Test
    public void parse_minimalUnicodeAndMixedOrder_createsNormalisedStudent() throws Exception {
        ModelManager model = new ModelManager();
        new AddressBookParser().parseCommand("\tstudent\tadd /github Alex-Tan /email ALEX@U.NUS.EDU"
                + " /telegram @Alex_Tan /name  José\t  王小明  ").execute(model);
        var person = model.getAddressBook().getPersonList().get(0);
        assertEquals("José 王小明", person.getName().fullName);
        assertEquals("alex@u.nus.edu", person.getEmail().value);
        assertEquals("Alex_Tan", person.getTelegram().orElseThrow().value);
        assertEquals("Alex-Tan", person.getGitHub().orElseThrow().value);
        assertEquals(false, person.isSample());
        assertEquals(0, person.getEnrolments().size());
    }

    @Test
    public void parse_structuralErrors_reportFirstBeforeValues() {
        syntax("text" + VALID, "Unexpected text before the first parameter.");
        syntax(VALID + " /name Other", "Parameter /name can be supplied only once.");
        syntax(VALID + " /phone 123", "Unknown parameter: /phone");
        syntax(" /unknown value /name /name Again", "Unknown parameter: /unknown");
        syntax(" /name /name Again /unknown value", "Parameter /name can be supplied only once.");
        for (String token : new String[] {"/", "/name=Alex", "/123", "//name", "/name/"}) {
            syntax(VALID + " " + token,
                    "Invalid parameter syntax. Use the parameter prefixes shown in the command format.");
        }
    }

    @Test
    public void parse_values_followDocumentedOrder() {
        syntax("", "Missing required parameter: /name");
        syntax(" /name Alex", "Missing required parameter: /email");
        syntax(" /name /email bad", "A value is required for /name.");
        syntax(VALID + " /telegram", "A value is required for /telegram.");
        syntax(VALID + " /github ", "A value is required for /github.");
        assertParseFailure(parser, " /name Alex/Tan /email bad", Name.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, " /name Alex /email bad /telegram bad", Email.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, VALID + " /telegram bad /github @bad", Telegram.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, VALID + " /github @bad", GitHub.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_boundariesAndControls_enforced() throws Exception {
        parser.parse(" /name " + "王".repeat(100) + " /email a@u.nus.edu");
        assertParseFailure(parser, " /name " + "王".repeat(101) + " /email a@u.nus.edu", Name.MESSAGE_CONSTRAINTS);
        for (String control : new String[] {"\n", "\r", "\0", "\u0085", "\u2028", "\u2029"}) {
            assertThrows(ParseException.class, () ->
                    new AddressBookParser().parseCommand("student add" + VALID + control));
        }
        for (String command : new String[] {"add n/Alex", "student Add" + VALID, "student addendum" + VALID}) {
            assertThrows(ParseException.class, () ->
                    new AddressBookParser().parseCommand(command));
        }
    }

    private void syntax(String input, String message) {
        assertParseFailure(parser, input, message + "\nUsage: " + AddCommand.MESSAGE_USAGE);
    }
}
