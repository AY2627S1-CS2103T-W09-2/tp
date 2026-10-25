package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;

/**
 * Rejects the retired whole-roster clear operation.
 */
public class ClearCommand extends Command {

    public static final String COMMAND_WORD = "clear";
    public static final String MESSAGE_RETIRED = "The clear command is unavailable because it would remove the "
            + "entire roster. Use delete EMAIL to remove one student profile or sample clear to remove fictional "
            + "sample profiles.";
    public static final String MESSAGE_USAGE = MESSAGE_RETIRED;

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        throw new CommandException(MESSAGE_RETIRED);
    }
}
