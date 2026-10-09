package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_PERSONS;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Email;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonOrder;

/** Selects one student by canonical NUS email so that the complete profile is displayed. */
public class ViewCommand extends Command {
    public static final String COMMAND_WORD = "view";
    public static final String MESSAGE_USAGE = "Usage: view EMAIL";
    public static final String MESSAGE_MISSING_EMAIL = "NUS email is required. " + MESSAGE_USAGE;
    public static final String MESSAGE_EXTRA_ARGUMENT = "View accepts one NUS email only. " + MESSAGE_USAGE;
    public static final String MESSAGE_SUCCESS = "Viewing student: %s.";
    public static final String MESSAGE_NOT_FOUND = "No student found with NUS email: %s.";
    public static final String MESSAGE_DISPLAY_FAILURE =
            "Profile could not be displayed. Try viewing the student again.";

    private final Email email;

    /** Creates a lookup for a validated canonical email. */
    public ViewCommand(Email email) {
        this.email = requireNonNull(email);
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public String getDisplayFailureMessage(String defaultMessage) {
        return MESSAGE_DISPLAY_FAILURE;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Person target = model.getAddressBook().getPersonList().stream()
                .filter(person -> person.getEmail().equals(email)).findFirst()
                .orElseThrow(() -> new CommandException(String.format(MESSAGE_NOT_FOUND, email)));
        // A visible target keeps the current search; a hidden one is revealed in the full sorted roster.
        if (!model.getFilteredPersonList().contains(target)) {
            model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS, PersonOrder.BY_NAME_THEN_EMAIL);
        }
        return CommandResult.forTarget(String.format(MESSAGE_SUCCESS, target.getName()), target);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || (other instanceof ViewCommand command && email.equals(command.email));
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("email", email).toString();
    }
}
