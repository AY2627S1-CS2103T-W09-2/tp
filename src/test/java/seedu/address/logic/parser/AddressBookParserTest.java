package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_ENTER_COMMAND;
import static seedu.address.logic.Messages.MESSAGE_SINGLE_LINE;
import static seedu.address.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static seedu.address.logic.commands.CommandTestUtil.ADDRESS_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.EMAIL_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.NAME_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.VALID_EMAIL_AMY;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.ClearCommand;
import seedu.address.logic.commands.ConfirmDeleteCommand;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.commands.ExitCommand;
import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.RemarkCommand;
import seedu.address.logic.commands.ViewCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.Email;
import seedu.address.model.person.Person;
import seedu.address.model.person.Remark;
import seedu.address.testutil.PersonBuilder;
import seedu.address.testutil.PersonUtil;

public class AddressBookParserTest {

    private final AddressBookParser parser = new AddressBookParser();

    @Test
    public void parseCommand_add() throws Exception {
        Person person = new PersonBuilder().build();
        AddCommand command = (AddCommand) parser.parseCommand(PersonUtil.getAddCommand(person));
        assertEquals(new AddCommand(person), command);
    }

    @Test
    public void parseCommand_clear() throws Exception {
        assertTrue(parser.parseCommand(ClearCommand.COMMAND_WORD) instanceof ClearCommand);
        assertTrue(parser.parseCommand(ClearCommand.COMMAND_WORD + " 3") instanceof ClearCommand);
    }

    @Test
    public void parseCommand_delete() throws Exception {
        Email email = new Email("e9000001@u.nus.edu");
        assertEquals(new DeleteCommand(email), parser.parseCommand("delete E9000001@U.NUS.EDU"));
        assertEquals(new DeleteCommand(email), parser.parseCommand(" \tdelete\te9000001@u.nus.edu\t"));
        assertThrows(ParseException.class, DeleteCommand.MESSAGE_MISSING_EMAIL, () -> parser.parseCommand("delete"));
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () ->
                parser.parseCommand("DELETE e9000001@u.nus.edu"));
    }

    @Test
    public void parseCommand_confirmDelete() throws Exception {
        assertEquals(new ConfirmDeleteCommand(new Email("e9000001@u.nus.edu")),
                parser.parseCommand("confirm-delete E9000001@U.NUS.EDU"));
        assertThrows(ParseException.class, ConfirmDeleteCommand.MESSAGE_MISSING_EMAIL, () ->
                parser.parseCommand("confirm-delete"));
        assertThrows(ParseException.class, MESSAGE_SINGLE_LINE, () ->
                parser.parseCommand("confirm-delete e9000001@u.nus.edu\n"));
    }


    @Test
    public void parseCommand_exit() throws Exception {
        for (String word : new String[] {"bye", "exit"}) {
            for (String input : new String[] {word, " \t" + word + "\t ", word + "   "}) {
                assertTrue(parser.parseCommand(input) instanceof ExitCommand, input);
            }
            for (String extra : new String[] {" 3", " now", "\tnow", " \t 3 \t"}) {
                assertThrows(ParseException.class, "Bye does not accept parameters. Usage: bye", () ->
                        parser.parseCommand(word + extra));
            }
            for (String control : new String[] {"\n", "\r", "\0", "\u2028"}) {
                assertThrows(ParseException.class, MESSAGE_SINGLE_LINE, () -> parser.parseCommand(word + control));
            }
            assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () ->
                    parser.parseCommand(word.toUpperCase()));
        }
    }

    @Test
    public void parseCommand_find() throws Exception {
        assertEquals(new FindCommand("foo bar baz"), parser.parseCommand("find foo bar baz"));
        assertEquals(new FindCommand("Alex  Tan"), parser.parseCommand(" \tfind\tAlex  Tan\t"));
        for (String command : new String[] {"find Alex\n", "\0find Alex", "find Alex\u2028Tan"}) {
            assertThrows(ParseException.class, MESSAGE_SINGLE_LINE, () -> parser.parseCommand(command));
        }
    }

    @Test
    public void parseCommand_controlAtEmailBoundary_throwsParseException() {
        String add = "student add /name Amy /email ";
        String edit = "edit /email ";
        String[] commands = {
            add + EMAIL_DESC_AMY + "\u0001" + ADDRESS_DESC_AMY, // before another prefix
            add + ADDRESS_DESC_AMY + EMAIL_DESC_AMY + "\u0001", // last argument and end of command
            edit + "\u000B" + VALID_EMAIL_AMY, // vertical tab before the email
            edit + VALID_EMAIL_AMY + "\r" + NAME_DESC_AMY, // carriage return
            edit + VALID_EMAIL_AMY + "\n" + NAME_DESC_AMY, // line feed
            edit + "\u0085" + VALID_EMAIL_AMY, // C1 control (next line)
            edit + VALID_EMAIL_AMY + "\u007F", // delete
            edit + VALID_EMAIL_AMY + "\u2029" // paragraph separator
        };
        for (String command : commands) {
            assertThrows(ParseException.class, MESSAGE_SINGLE_LINE, () -> parser.parseCommand(command));
        }
    }


    @Test
    public void parseCommand_help() throws Exception {
        assertTrue(parser.parseCommand(HelpCommand.COMMAND_WORD) instanceof HelpCommand);
        assertTrue(parser.parseCommand(HelpCommand.COMMAND_WORD + " 3") instanceof HelpCommand);
    }

    @Test
    public void parseCommand_list() throws Exception {
        assertTrue(parser.parseCommand(ListCommand.COMMAND_WORD) instanceof ListCommand);
        assertTrue(parser.parseCommand(ListCommand.COMMAND_WORD + " 3") instanceof ListCommand);
    }

    @Test
    public void parseCommand_view() throws Exception {
        assertEquals(new ViewCommand(new Email("e9000001@u.nus.edu")), parser.parseCommand("view E9000001@U.NUS.EDU"));
        assertThrows(ParseException.class, ViewCommand.MESSAGE_MISSING_EMAIL, () -> parser.parseCommand("view"));
    }

    @Test
    public void parseCommand_remark() throws Exception {
        RemarkCommand command = (RemarkCommand) parser.parseCommand("remark 1 r/Likes baseball");
        assertEquals(new RemarkCommand(INDEX_FIRST_PERSON, new Remark("Likes baseball")), command);
    }

    @Test
    public void parseCommand_blankInput_throwsEnterCommand() {
        for (String blank : new String[] {"", " ", " \t "}) {
            assertThrows(ParseException.class, MESSAGE_ENTER_COMMAND, () -> parser.parseCommand(blank));
        }
    }

    @Test
    public void parseCommand_unknownCommand_throwsParseException() {
        assertThrows(ParseException.class, MESSAGE_UNKNOWN_COMMAND, () -> parser.parseCommand("unknownCommand"));
    }
}
