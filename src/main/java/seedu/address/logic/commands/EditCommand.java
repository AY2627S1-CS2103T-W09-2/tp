package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_PERSONS;

import java.util.Optional;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Email;
import seedu.address.model.person.GitHub;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonOrder;
import seedu.address.model.person.Telegram;

/** Replaces or clears optional contacts while preserving all other stored fields. */
public class EditCommand extends Command {
    public static final String COMMAND_WORD = "edit";
    public static final String MESSAGE_USAGE = "edit /email EMAIL [/telegram HANDLE|clear] [/github USERNAME|clear]";
    public static final String MESSAGE_EDIT_PERSON_SUCCESS = "Updated student: %s.";
    public static final String MESSAGE_NOT_EDITED = "Provide at least one field to update.";
    public static final String MESSAGE_NO_CHANGE = "No values changed.";
    private final Email email;
    // Outer absence means preserve; inner absence means explicitly clear.
    private final Optional<Optional<Telegram>> telegram;
    private final Optional<Optional<GitHub>> github;

    /** Creates a contact update; an outer absent value preserves the existing contact. */
    public EditCommand(Email email, Optional<Optional<Telegram>> telegram, Optional<Optional<GitHub>> github) {
        this.email = requireNonNull(email);
        this.telegram = requireNonNull(telegram);
        this.github = requireNonNull(github);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        Person target = model.getAddressBook().getPersonList().stream()
                .filter(person -> person.getEmail().equals(email)).findFirst()
                .orElseThrow(() -> new CommandException("No student found with NUS email: " + email + "."));
        Person updated = target.withContacts(telegram.orElse(target.getTelegram()), github.orElse(target.getGitHub()));
        boolean unchanged = target.equals(updated);
        if (!unchanged) {
            model.setPerson(target, updated);
        }
        model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS, PersonOrder.BY_NAME_THEN_EMAIL);
        return CommandResult.forTarget(unchanged ? MESSAGE_NO_CHANGE
                : String.format(MESSAGE_EDIT_PERSON_SUCCESS, updated.getName()), updated);
    }

    @Override
    public String getSaveFailureMessage(String defaultMessage) {
        return "The record could not be updated. No data was changed.";
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof EditCommand command && email.equals(command.email)
                && telegram.equals(command.telegram) && github.equals(command.github);
    }
}
