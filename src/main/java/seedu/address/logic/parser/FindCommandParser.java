package seedu.address.logic.parser;

import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses one literal query for any of the four student identifiers.
 */
public class FindCommandParser implements Parser<FindCommand> {
    @Override
    public FindCommand parse(String args) throws ParseException {
        ParserUtil.requireSingleLine(args);
        String query = args.replaceAll("^[ \t]+|[ \t]+$", "").replace('\t', ' ');
        if (query.isEmpty()) {
            throw new ParseException(FindCommand.MESSAGE_EMPTY_QUERY + "\n" + FindCommand.MESSAGE_USAGE);
        }
        if (query.codePointCount(0, query.length()) > 100) {
            throw new ParseException(FindCommand.MESSAGE_LONG_QUERY);
        }
        return new FindCommand(query);
    }
}
