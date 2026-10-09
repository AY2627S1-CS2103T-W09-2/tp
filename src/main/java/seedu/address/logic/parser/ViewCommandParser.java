package seedu.address.logic.parser;

import seedu.address.logic.commands.ViewCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses exactly one NUS email. The argument count is checked before the email is validated.
 */
public class ViewCommandParser implements Parser<ViewCommand> {
    @Override
    public ViewCommand parse(String args) throws ParseException {
        ParserUtil.requireSingleLine(args);
        String trimmed = args.replaceAll("^[ \t]+|[ \t]+$", "");
        if (trimmed.isEmpty()) {
            throw new ParseException(ViewCommand.MESSAGE_MISSING_EMAIL);
        }
        if (trimmed.split("[ \t]+").length > 1) {
            throw new ParseException(ViewCommand.MESSAGE_EXTRA_ARGUMENT);
        }
        return new ViewCommand(ParserUtil.parseEmail(trimmed));
    }
}
