package seedu.address.logic.parser;

import static seedu.address.logic.parser.CliSyntax.STUDENT_EMAIL;
import static seedu.address.logic.parser.CliSyntax.STUDENT_GITHUB;
import static seedu.address.logic.parser.CliSyntax.STUDENT_NAME;
import static seedu.address.logic.parser.CliSyntax.STUDENT_TELEGRAM;

import java.util.Optional;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.Email;
import seedu.address.model.person.GitHub;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Telegram;

/** Parses the parameters following student add. */
public class AddCommandParser implements Parser<AddCommand> {
    @Override
    public AddCommand parse(String args) throws ParseException {
        SlashArguments values = SlashPrefixTokenizer.tokenize(args, AddCommand.MESSAGE_USAGE,
                STUDENT_NAME, STUDENT_EMAIL, STUDENT_TELEGRAM, STUDENT_GITHUB);
        Name name = ParserUtil.parseName(values.getRequired(STUDENT_NAME));
        Email email = ParserUtil.parseEmail(values.getRequired(STUDENT_EMAIL));
        Optional<Telegram> telegram = parseTelegram(values.getOptional(STUDENT_TELEGRAM));
        Optional<GitHub> github = parseGitHub(values.getOptional(STUDENT_GITHUB));
        return new AddCommand(new Person(name, email, telegram, github));
    }

    /** Parses an optional contact, preserving absence. */
    static Optional<Telegram> parseTelegram(Optional<String> value) throws ParseException {
        if (value.isPresent() && !Telegram.isValidTelegram(value.get())) {
            throw new ParseException(Telegram.MESSAGE_CONSTRAINTS);
        }
        return value.map(Telegram::new);
    }

    /** Parses an optional contact, preserving absence. */
    static Optional<GitHub> parseGitHub(Optional<String> value) throws ParseException {
        if (value.isPresent() && !GitHub.isValidGitHub(value.get())) {
            throw new ParseException(GitHub.MESSAGE_CONSTRAINTS);
        }
        return value.map(GitHub::new);
    }
}
