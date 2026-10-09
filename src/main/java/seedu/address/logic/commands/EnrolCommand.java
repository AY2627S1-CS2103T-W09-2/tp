package seedu.address.logic.commands;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_PERSONS;

import java.util.ArrayList;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.person.Email;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonOrder;

/** Adds one teaching context to an existing student without replacing earlier enrolments. */
public class EnrolCommand extends Command {
    public static final String COMMAND_WORD = "enrol";
    public static final String MESSAGE_USAGE =
            "enrol /email EMAIL /module MODULE /semester SEMESTER [/section SECTION] [/team TEAM]";
    public static final String MESSAGE_SUCCESS = "Added enrolment for %s: %s, %s.";
    public static final String MESSAGE_MISSING_STUDENT = "No student found with NUS email: %s. "
            + "Create the student profile before adding an enrolment.";
    public static final String MESSAGE_DUPLICATE = "This student already has an enrolment for %s, %s. "
            + "Use edit-enrol to change its section or team.";
    public static final String MESSAGE_SAVE_FAILURE = "The enrollment could not be saved. No data was changed.";

    private final Email email;
    private final Enrolment enrolment;

    /** Creates an enrolment request with validated canonical identity and context. */
    public EnrolCommand(Email email, Enrolment enrolment) {
        requireAllNonNull(email, enrolment);
        this.email = email;
        this.enrolment = enrolment;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        Person target = model.getAddressBook().getPersonList().stream()
                .filter(person -> person.getEmail().equals(email)).findFirst()
                .orElseThrow(() -> new CommandException(String.format(MESSAGE_MISSING_STUDENT, email)));
        if (target.getEnrolments().stream().anyMatch(enrolment::hasSameKey)) {
            throw new CommandException(String.format(MESSAGE_DUPLICATE,
                    enrolment.getModuleCode(), enrolment.getSemester()));
        }
        ArrayList<Enrolment> contexts = new ArrayList<>(target.getEnrolments());
        contexts.add(enrolment);
        Person updated = target.withEnrolments(contexts);
        model.setPerson(target, updated);
        model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS, PersonOrder.BY_NAME_THEN_EMAIL);
        String feedback = String.format(MESSAGE_SUCCESS, target.getName(), enrolment.getModuleCode(),
                enrolment.getSemester()) + "\nModule: " + enrolment.getModuleCode()
                + "\nSemester: " + enrolment.getSemester()
                + "\nSection: " + enrolment.getSection().map(Object::toString).orElse("Not assigned")
                + "\nTeam: " + enrolment.getTeam().map(Object::toString).orElse("Not assigned");
        return CommandResult.forTarget(feedback, updated);
    }

    @Override
    public String getSaveFailureMessage(String defaultMessage) {
        return MESSAGE_SAVE_FAILURE;
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof EnrolCommand command
                && email.equals(command.email) && enrolment.equals(command.enrolment);
    }
}
