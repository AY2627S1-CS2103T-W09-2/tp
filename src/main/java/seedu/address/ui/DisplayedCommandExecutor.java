package seedu.address.ui;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import seedu.address.logic.Logic;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Keeps command targets consistent with the displayed snapshot, including after failed commands.
 * Display callbacks are supplied by the window so this coordination can be tested without a toolkit.
 */
class DisplayedCommandExecutor {
    static final String MESSAGE_DISPLAY_CHANGED = "The student list has been refreshed. "
            + "Check the displayed indexes and submit your command again.";
    static final String MESSAGE_DISPLAY_FAILURE = "The student list could not be refreshed. "
            + "No further commands will run until it can be displayed. Retry to refresh the list.";

    private final Logic logic;
    private final BooleanSupplier isDisplayCurrent;
    private final Consumer<CommandResult> presentSearch;
    private final Runnable refreshDisplay;

    DisplayedCommandExecutor(Logic logic, BooleanSupplier isDisplayCurrent,
            Consumer<CommandResult> presentSearch, Runnable refreshDisplay) {
        this.logic = logic;
        this.isDisplayCurrent = isDisplayCurrent;
        this.presentSearch = presentSearch;
        this.refreshDisplay = refreshDisplay;
    }

    CommandResult execute(String commandText) throws CommandException, ParseException {
        if (!isDisplayCurrent.getAsBoolean()) {
            // A rejected submission still counts as the next command, so it cancels any pending deletion first.
            boolean isDeletionCancelled = logic.cancelPendingDeletion();
            try {
                refreshIfChanged();
            } catch (CommandException displayFailure) {
                throw withCancellationNotice(isDeletionCancelled, displayFailure);
            }
            // The submitted index refers to the old display. Never execute it against newly refreshed rows.
            throw withCancellationNotice(isDeletionCancelled, new CommandException(MESSAGE_DISPLAY_CHANGED));
        }

        CommandResult result;
        try {
            result = logic.execute(commandText, presentSearch);
        } catch (CommandException | ParseException e) {
            try {
                refreshIfChanged();
            } catch (CommandException displayFailure) {
                throw new CommandException(e.getMessage() + "\n" + displayFailure.getMessage(), e);
            }
            throw e;
        }
        refreshIfChanged();
        return result;
    }

    private static CommandException withCancellationNotice(boolean isDeletionCancelled, CommandException e) {
        if (!isDeletionCancelled) {
            return e;
        }
        return new CommandException(Messages.MESSAGE_PENDING_DELETION_CANCELLED + "\n" + e.getMessage(), e);
    }

    private void refreshIfChanged() throws CommandException {
        if (isDisplayCurrent.getAsBoolean()) {
            return;
        }
        try {
            refreshDisplay.run();
        } catch (RuntimeException | AssertionError e) {
            throw new CommandException(MESSAGE_DISPLAY_FAILURE, e);
        }
    }
}
