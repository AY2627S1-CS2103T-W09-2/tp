package seedu.address.logic.commands;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;

/**
 * Represents a command with hidden internal logic and the ability to be executed.
 */
public abstract class Command {

    /**
     * Returns whether execution leaves the stored roster unchanged.
     */
    public boolean isReadOnly() {
        return false;
    }

    /** Returns the feedback after a save failure has restored the previous state. */
    public String getSaveFailureMessage(String defaultMessage) {
        return defaultMessage;
    }

    /** Returns the feedback after a failed display has restored the previous results and selection. */
    public String getDisplayFailureMessage(String defaultMessage) {
        return defaultMessage;
    }

    /**
     * Executes the command and returns the result message.
     *
     * @param model {@code Model} which the command should operate on.
     * @return feedback message of the operation result for display
     * @throws CommandException If an error occurs during command execution.
     */
    public abstract CommandResult execute(Model model) throws CommandException;

}
