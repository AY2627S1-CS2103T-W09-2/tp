package seedu.address.logic.parser;

import seedu.address.logic.commands.ConfirmDeleteCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses exactly one NUS email for a deletion confirmation. The argument count is checked before the email is
 * validated.
 */
public class ConfirmDeleteCommandParser implements Parser<ConfirmDeleteCommand> {

    @Override
    public ConfirmDeleteCommand parse(String args) throws ParseException {
        return new ConfirmDeleteCommand(ParserUtil.parseSoleEmail(args, ConfirmDeleteCommand.MESSAGE_MISSING_EMAIL,
                ConfirmDeleteCommand.MESSAGE_EXTRA_ARGUMENT));
    }
}
