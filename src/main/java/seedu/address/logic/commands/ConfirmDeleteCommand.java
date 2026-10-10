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
 * Deletes the student previewed by {@link DeleteCommand}, together with every enrolment it owns.
 * Every confirmation attempt consumes the pending deletion, whether or not it succeeds.
 */
public class ConfirmDeleteCommand extends Command {

    public static final String COMMAND_WORD = "confirm-delete";
    public static final String MESSAGE_USAGE = "Usage: confirm-delete EMAIL";
    public static final String MESSAGE_MISSING_EMAIL = "NUS email is required. " + MESSAGE_USAGE;
    public static final String MESSAGE_EXTRA_ARGUMENT = "Confirm-delete accepts one NUS email only. " + MESSAGE_USAGE;
    public static final String MESSAGE_NO_PENDING = "There is no pending deletion. Use delete EMAIL first.";
    public static final String MESSAGE_MISMATCH =
            "Deletion was cancelled because the confirmation did not match the selected student.";
    public static final String MESSAGE_TARGET_MISSING = "The pending student no longer exists. No data was changed.";
    public static final String MESSAGE_SUCCESS = "Deleted student: %s. NUS email: %s. Enrolments removed: %d.";
    public static final String MESSAGE_FAILURE =
            "The student could not be deleted. No data was changed. Use delete EMAIL to start again.";

    private final Email email;

    /** Creates a confirmation for a validated canonical email. */
    public ConfirmDeleteCommand(Email email) {
        this.email = requireNonNull(email);
    }

    @Override
    public boolean isDeletionConfirmation() {
        return true;
    }

    @Override
    public String getSaveFailureMessage(String defaultMessage) {
        return MESSAGE_FAILURE;
    }

    @Override
    public String getDisplayFailureMessage(String defaultMessage) {
        return MESSAGE_FAILURE;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Optional<Email> pendingEmail = model.getPendingDeletion();
        model.clearPendingDeletion();
        if (pendingEmail.isEmpty()) {
            throw new CommandException(MESSAGE_NO_PENDING);
        }
        if (!pendingEmail.get().equals(email)) {
            throw new CommandException(MESSAGE_MISMATCH);
        }
        Person target = DeleteCommand.findByEmail(model, email)
                .orElseThrow(() -> new CommandException(MESSAGE_TARGET_MISSING));

        // The enrolments belong to the profile, so removing it removes them in the same saved change.
        model.deletePerson(target);
        model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS, PersonOrder.BY_NAME_THEN_EMAIL);
        return CommandResult.forClearedSelection(String.format(MESSAGE_SUCCESS, target.getName(), target.getEmail(),
                target.getEnrolments().size()));
    }

    @Override
    public boolean equals(Object other) {
        return other == this || (other instanceof ConfirmDeleteCommand command && email.equals(command.email));
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
