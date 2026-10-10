package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_PERSONS;

import java.util.Optional;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Email;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonOrder;

/**
 * Previews one student for deletion by canonical NUS email. Nothing is removed until a matching
 * {@link ConfirmDeleteCommand} is the next submitted command.
 */
public class DeleteCommand extends Command {

    public static final String COMMAND_WORD = "delete";
    public static final String MESSAGE_USAGE = "Usage: delete EMAIL";
    public static final String MESSAGE_MISSING_EMAIL = "NUS email is required. " + MESSAGE_USAGE;
    public static final String MESSAGE_EXTRA_ARGUMENT = "Delete accepts one NUS email only. " + MESSAGE_USAGE;
    public static final String MESSAGE_NOT_FOUND = "No student found with NUS email: %s.";
    public static final String MESSAGE_PREVIEW = "Delete this student?\n%s\nNUS email: %s\nEnrolments to remove: %d\n"
            + "Type: " + ConfirmDeleteCommand.COMMAND_WORD + " %s\nAny other command will cancel this deletion.";

    private final Email email;

    /** Creates a deletion preview for a validated canonical email. */
    public DeleteCommand(Email email) {
        this.email = requireNonNull(email);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Person target = findByEmail(model, email)
                .orElseThrow(() -> new CommandException(String.format(MESSAGE_NOT_FOUND, email)));
        // A visible target keeps the current search; a hidden one is revealed in the full sorted roster.
        if (!model.getFilteredPersonList().contains(target)) {
            model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS, PersonOrder.BY_NAME_THEN_EMAIL);
        }
        model.setPendingDeletion(target.getEmail());
        String preview = String.format(MESSAGE_PREVIEW, target.getName(), target.getEmail(),
                target.getEnrolments().size(), target.getEmail());
        return CommandResult.forTarget(preview, target);
    }

    /** Returns the profile in the complete roster whose canonical email is {@code email}, if any. */
    static Optional<Person> findByEmail(Model model, Email email) {
        return model.getAddressBook().getPersonList().stream()
                .filter(person -> person.getEmail().equals(email))
                .findFirst();
    }

    @Override
    public boolean equals(Object other) {
        return other == this || (other instanceof DeleteCommand command && email.equals(command.email));
    }

    @Override
    public int hashCode() {
        return email.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("email", email).toString();
    }
}
