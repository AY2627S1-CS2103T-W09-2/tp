package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_ENTER_COMMAND;
import static seedu.address.logic.Messages.MESSAGE_UNKNOWN_COMMAND;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.ClearCommand;
import seedu.address.logic.commands.ClearSamplesCommand;
import seedu.address.logic.commands.Command;
import seedu.address.logic.commands.ConfirmDeleteCommand;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.commands.EditCommand;
import seedu.address.logic.commands.EnrolCommand;
import seedu.address.logic.commands.ExitCommand;
import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.RemarkCommand;
import seedu.address.logic.commands.SampleCommand;
import seedu.address.logic.commands.ViewCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses user input.
 */
public class AddressBookParser {

    /**
     * Used for initial separation of command word and args.
     */
    private static final Pattern BASIC_COMMAND_FORMAT = Pattern.compile("(?<commandWord>\\S+)(?<arguments>.*)");
    private static final Logger logger = LogsCenter.getLogger(AddressBookParser.class);

    /**
     * Parses user input into command for execution.
     *
     * @param userInput full user input string
     * @return the command based on the user input
     * @throws ParseException if the user input does not conform to the expected format
     */
    public Command parseCommand(String userInput) throws ParseException {
        ParserUtil.requireSingleLine(userInput);
        final Matcher matcher = BASIC_COMMAND_FORMAT.matcher(userInput.trim());
        if (!matcher.matches()) {
            // Only blank input has no command word.
            throw new ParseException(MESSAGE_ENTER_COMMAND);
        }

        final String commandWord = matcher.group("commandWord");
        final String arguments = matcher.group("arguments");

        // Note to developers: Change LOG_LEVEL in LogsCenter to enable lower level (i.e., FINE, FINER and lower)
        // log messages such as the one below.
        // Lower level log messages are used sparingly to minimize noise in the code.
        logger.fine("Command word: " + commandWord + "; Arguments: " + arguments);

        return switch (commandWord) {
            case AddCommand.COMMAND_WORD -> parseStudent(arguments);
            case EnrolCommand.COMMAND_WORD -> new EnrolCommandParser().parse(arguments);
            case EditCommand.COMMAND_WORD -> new EditCommandParser().parse(arguments);
            case DeleteCommand.COMMAND_WORD -> new DeleteCommandParser().parse(arguments);
            case ConfirmDeleteCommand.COMMAND_WORD -> new ConfirmDeleteCommandParser().parse(arguments);
            case ClearCommand.COMMAND_WORD -> new ClearCommand();
            case FindCommand.COMMAND_WORD -> new FindCommandParser().parse(arguments);
            case RemarkCommand.COMMAND_WORD -> new RemarkCommandParser().parse(arguments);
            case SampleCommand.COMMAND_WORD -> parseSample(arguments);
            case ViewCommand.COMMAND_WORD -> new ViewCommandParser().parse(arguments);
            case ListCommand.COMMAND_WORD -> new ListCommand();
            case ExitCommand.COMMAND_WORD, ExitCommand.COMMAND_WORD_ALIAS -> parseExit(arguments);
            case HelpCommand.COMMAND_WORD -> new HelpCommand();
            default -> {
                logger.finer("This user input caused a ParseException: " + userInput);
                throw new ParseException(MESSAGE_UNKNOWN_COMMAND);
            }
        };
    }

    private Command parseStudent(String args) throws ParseException {
        String stripped = args.trim();
        if (!stripped.matches("add(?:[ \\t].*)?")) {
            throw new ParseException("Usage: " + AddCommand.MESSAGE_USAGE);
        }
        return new AddCommandParser().parse(stripped.substring(3));
    }

    /** Accepts {@code bye} or {@code exit} only without parameters; surrounding spaces and tabs are already removed. */
    private Command parseExit(String args) throws ParseException {
        if (!args.trim().isEmpty()) {
            throw new ParseException(ExitCommand.MESSAGE_USAGE);
        }
        return new ExitCommand();
    }

    private Command parseSample(String args) throws ParseException {
        String stripped = args.trim();
        if (stripped.equals("load")) {
            return new SampleCommand();
        }
        if (stripped.matches("load[ \\t].*")) {
            throw new ParseException(SampleCommand.MESSAGE_USAGE);
        }
        if (stripped.equals("clear")) {
            return new ClearSamplesCommand();
        }
        if (stripped.matches("clear[ \\t].*")) {
            throw new ParseException(ClearSamplesCommand.MESSAGE_USAGE);
        }
        throw new ParseException(SampleCommand.MESSAGE_SUBCOMMAND_USAGE);
    }
}
