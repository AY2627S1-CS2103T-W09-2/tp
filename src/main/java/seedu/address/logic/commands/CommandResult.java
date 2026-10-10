package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.person.Person;

/**
 * Represents the result of a command execution.
 */
public class CommandResult {

    private final String feedbackToUser;

    /** Help information should be shown to the user. */
    private final boolean showHelp;

    /** The application should exit. */
    private final boolean exit;

    private final boolean updateSelection;
    private final Person selectionTarget;

    /** The refreshed results must be shown with no selected profile, even if only one remains. */
    private final boolean clearSelection;

    /**
     * Constructs a {@code CommandResult} with the specified fields.
     */
    public CommandResult(String feedbackToUser, boolean showHelp, boolean exit) {
        this(feedbackToUser, showHelp, exit, false, null, false);
    }

    private CommandResult(String feedbackToUser, boolean showHelp, boolean exit, boolean updateSelection,
            Person selectionTarget, boolean clearSelection) {
        assert !clearSelection || updateSelection && selectionTarget == null;
        this.feedbackToUser = requireNonNull(feedbackToUser);
        this.showHelp = showHelp;
        this.exit = exit;
        this.updateSelection = updateSelection;
        this.selectionTarget = selectionTarget;
        this.clearSelection = clearSelection;
    }

    /**
     * Constructs a {@code CommandResult} with the specified {@code feedbackToUser},
     * and other fields set to their default value.
     */
    public CommandResult(String feedbackToUser) {
        this(feedbackToUser, false, false);
    }

    /**
     * Returns a search result that selects the sole displayed match, or clears selection otherwise.
     */
    public static CommandResult forSearch(String feedbackToUser) {
        return new CommandResult(feedbackToUser, false, false, true, null, false);
    }

    /**
     * Returns whether the UI should select the sole result or clear selection after a search.
     */
    public boolean isUpdateSelection() {
        return updateSelection;
    }

    /** Requests selection of an explicit profile, even in a multi-row roster. */
    public static CommandResult forTarget(String feedback, Person target) {
        return new CommandResult(feedback, false, false, true, requireNonNull(target), false);
    }

    /**
     * Returns a result that presents the refreshed results with no selected profile, even if exactly one remains.
     * Like other selection updates, it is presented before the change is saved.
     */
    public static CommandResult forClearedSelection(String feedback) {
        return new CommandResult(feedback, false, false, true, null, true);
    }

    /**
     * Returns a copy whose feedback starts with {@code notice} on its own line. Every other field is kept.
     */
    public CommandResult withNotice(String notice) {
        requireNonNull(notice);
        return new CommandResult(notice + "\n" + feedbackToUser, showHelp, exit, updateSelection, selectionTarget,
                clearSelection);
    }

    public Person getSelectionTarget() {
        return selectionTarget;
    }

    public boolean isClearSelection() {
        return clearSelection;
    }

    public String getFeedbackToUser() {
        return feedbackToUser;
    }

    public boolean isShowHelp() {
        return showHelp;
    }

    public boolean isExit() {
        return exit;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof CommandResult otherCommandResult)) {
            return false;
        }

        return feedbackToUser.equals(otherCommandResult.feedbackToUser)
                && showHelp == otherCommandResult.showHelp
                && exit == otherCommandResult.exit
                && updateSelection == otherCommandResult.updateSelection
                && Objects.equals(selectionTarget, otherCommandResult.selectionTarget)
                && clearSelection == otherCommandResult.clearSelection;
    }

    @Override
    public int hashCode() {
        return Objects.hash(feedbackToUser, showHelp, exit, updateSelection, selectionTarget, clearSelection);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("feedbackToUser", feedbackToUser)
                .add("showHelp", showHelp)
                .add("exit", exit)
                .add("updateSelection", updateSelection)
                .add("clearSelection", clearSelection)
                .toString();
    }

}
