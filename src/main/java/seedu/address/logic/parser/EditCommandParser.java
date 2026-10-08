package seedu.address.logic.parser;

import static seedu.address.logic.parser.CliSyntax.STUDENT_EMAIL;
import static seedu.address.logic.parser.CliSyntax.STUDENT_GITHUB;
import static seedu.address.logic.parser.CliSyntax.STUDENT_TELEGRAM;

import java.util.Optional;

import seedu.address.logic.commands.EditCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.Email;
import seedu.address.model.person.GitHub;
import seedu.address.model.person.Telegram;

/** Parses the contact-only subset of edit. */
public class EditCommandParser implements Parser<EditCommand> {
    @Override
    public EditCommand parse(String args) throws ParseException {
        SlashArguments values = SlashPrefixTokenizer.tokenize(args, EditCommand.MESSAGE_USAGE,
                STUDENT_EMAIL, STUDENT_TELEGRAM, STUDENT_GITHUB);
        Email email = ParserUtil.parseEmail(values.getRequired(STUDENT_EMAIL));
        Optional<String> telegramValue = values.getOptional(STUDENT_TELEGRAM);
        Optional<Optional<Telegram>> telegram = Optional.empty();
        if (telegramValue.isPresent()) {
            telegram = Optional.of(telegramValue.get().equals("clear") ? Optional.empty()
                    : AddCommandParser.parseTelegram(telegramValue));
        }
        Optional<String> githubValue = values.getOptional(STUDENT_GITHUB);
        Optional<Optional<GitHub>> github = Optional.empty();
        if (githubValue.isPresent()) {
            github = Optional.of(githubValue.get().equals("clear") ? Optional.empty()
                    : AddCommandParser.parseGitHub(githubValue));
        }
        if (telegram.isEmpty() && github.isEmpty()) {
            throw new ParseException(EditCommand.MESSAGE_NOT_EDITED);
        }
        return new EditCommand(email, telegram, github);
    }
}
