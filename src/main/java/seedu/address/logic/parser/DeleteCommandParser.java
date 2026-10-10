package seedu.address.logic.parser;

import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses exactly one NUS email for a deletion preview. The argument count is checked before the email is validated.
 */
public class DeleteCommandParser implements Parser<DeleteCommand> {

    @Override
    public DeleteCommand parse(String args) throws ParseException {
        return new DeleteCommand(ParserUtil.parseSoleEmail(args, DeleteCommand.MESSAGE_MISSING_EMAIL,
                DeleteCommand.MESSAGE_EXTRA_ARGUMENT));
    }
}
