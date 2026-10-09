package seedu.address.logic.parser;

import static seedu.address.logic.parser.CliSyntax.ENROL_MODULE;
import static seedu.address.logic.parser.CliSyntax.ENROL_SECTION;
import static seedu.address.logic.parser.CliSyntax.ENROL_SEMESTER;
import static seedu.address.logic.parser.CliSyntax.ENROL_TEAM;
import static seedu.address.logic.parser.CliSyntax.STUDENT_EMAIL;

import java.util.Optional;

import seedu.address.logic.commands.EnrolCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Section;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.enrolment.Team;
import seedu.address.model.person.Email;

/** Parses structural errors first, then values in the documented parameter order. */
public class EnrolCommandParser implements Parser<EnrolCommand> {
    @Override
    public EnrolCommand parse(String args) throws ParseException {
        SlashArguments values = SlashPrefixTokenizer.tokenize(args, EnrolCommand.MESSAGE_USAGE,
                STUDENT_EMAIL, ENROL_MODULE, ENROL_SEMESTER, ENROL_SECTION, ENROL_TEAM);
        try {
            Email email = ParserUtil.parseEmail(values.getRequired(STUDENT_EMAIL));
            ModuleCode module = new ModuleCode(values.getRequired(ENROL_MODULE));
            Semester semester = new Semester(values.getRequired(ENROL_SEMESTER));
            Optional<Section> section = values.getOptional(ENROL_SECTION).map(Section::new);
            Optional<Team> team = values.getOptional(ENROL_TEAM).map(Team::new);
            return new EnrolCommand(email, new Enrolment(module, semester, section, team));
        } catch (IllegalArgumentException e) {
            throw new ParseException(e.getMessage(), e);
        }
    }
}
