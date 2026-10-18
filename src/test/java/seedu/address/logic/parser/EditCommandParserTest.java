package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.EditCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Email;
import seedu.address.model.person.GitHub;
import seedu.address.model.person.Name;
import seedu.address.model.person.Telegram;

public class EditCommandParserTest {
    private static final String TARGET = " /email alex@u.nus.edu";
    private static final String SUPPLEMENTARY = "\uD840\uDC00"; // U+20000, one code point stored in two chars
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
        for (String prefix : new String[] {"/section", "/team", "/module", "/semester"}) {
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

    @Test
    public void parse_identityFields_acceptedAloneTogetherAndInAnyOrder() {
        assertParseSuccess(parser, TARGET + " /new-email \tNEW@U.NUS.EDU ",
                edit(Optional.of(new Email("new@u.nus.edu")), Optional.empty(), Optional.empty(), Optional.empty()));
        assertParseSuccess(parser, TARGET + " /name clear", nameOnly("clear")); // ordinary name text
        assertParseSuccess(parser, " /name Alex \t Lim /github clear /new-email e7654321@u.nus.edu" + TARGET,
                edit(Optional.of(new Email("e7654321@u.nus.edu")), Optional.of(new Name("Alex Lim")),
                        Optional.empty(), Optional.of(Optional.empty())));
    }

    @Test
    public void parse_nameCodePointBoundaries_matchCreation() {
        assertParseSuccess(parser, TARGET + " /name " + SUPPLEMENTARY.repeat(100),
                nameOnly(SUPPLEMENTARY.repeat(100)));
        // Surrounding tabs are removed and the internal space/tab run becomes one space: 100 code points.
        assertParseSuccess(parser, TARGET + " /name \t" + "a".repeat(50) + " \t " + "b".repeat(49) + "\t",
                nameOnly("a".repeat(50) + " " + "b".repeat(49)));
        assertParseFailure(parser, TARGET + " /name " + SUPPLEMENTARY.repeat(101), Name.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, TARGET + " /name " + "a".repeat(50) + "  " + "b".repeat(50),
                Name.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, TARGET + " /name Alex/Tan", Name.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, TARGET + " /name \u200B", Name.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void parse_identityErrors_structureFirstThenDocumentedFieldOrder() {
        // Structural errors precede every value error, whatever the values.
        syntax(" /email bad /new-email bad /new-email x", "Parameter /new-email can be supplied only once.");
        syntax(" /email bad /name Alex/Tan /nickname x", "Unknown parameter: /nickname");
        syntax(" /new-email new@u.nus.edu /name Alex", "Missing required parameter: /email");
        // An empty replacement keeps the shared usage error and is never labelled as an invalid new email.
        syntax(TARGET + " /new-email", "A value is required for /new-email.");
        syntax(TARGET + " /new-email \t /name Alex", "A value is required for /new-email.");
        syntax(TARGET + " /name", "A value is required for /name.");
        // Values are read in the order email, new email, name, Telegram, GitHub, regardless of typed order.
        assertParseFailure(parser, " /new-email bad /email bad", Email.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, " /email bad /new-email", Email.MESSAGE_CONSTRAINTS);
        assertParseFailure(parser, TARGET + " /name Alex/Tan /new-email bad", EditCommand.MESSAGE_INVALID_NEW_EMAIL);
        assertParseFailure(parser, TARGET + " /new-email bad /name", EditCommand.MESSAGE_INVALID_NEW_EMAIL);
        syntax(TARGET + " /new-email /name Alex/Tan", "A value is required for /new-email.");
        assertParseFailure(parser, TARGET + " /telegram four /name Alex/Tan", Name.MESSAGE_CONSTRAINTS);
        syntax(TARGET + " /github bad_ /name", "A value is required for /name.");
        assertParseFailure(parser, TARGET + " /new-email clear", EditCommand.MESSAGE_INVALID_NEW_EMAIL);
        assertEquals("New email: " + Email.MESSAGE_CONSTRAINTS, EditCommand.MESSAGE_INVALID_NEW_EMAIL);
    }

    private EditCommand nameOnly(String name) {
        return edit(Optional.empty(), Optional.of(new Name(name)), Optional.empty(), Optional.empty());
    }

    private EditCommand edit(Optional<Email> newEmail, Optional<Name> name, Optional<Optional<Telegram>> telegram,
            Optional<Optional<GitHub>> github) {
        return new EditCommand(new Email("alex@u.nus.edu"), newEmail, name, telegram, github);
    }

    private void syntax(String input, String message) {
        assertParseFailure(parser, input, message + "\nUsage: " + EditCommand.MESSAGE_USAGE);
    }
}
