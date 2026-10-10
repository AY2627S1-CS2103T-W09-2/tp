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
    private final boolean preserveSurvivingSelection;

    /**
     * Constructs a {@code CommandResult} with the specified fields.
     */
    public CommandResult(String feedbackToUser, boolean showHelp, boolean exit) {
        this(feedbackToUser, showHelp, exit, false, null, false);
    }

    private CommandResult(String feedbackToUser, boolean showHelp, boolean exit, boolean updateSelection,
            Person selectionTarget, boolean preserveSurvivingSelection) {
        this.feedbackToUser = requireNonNull(feedbackToUser);
        this.showHelp = showHelp;
        this.exit = exit;
        this.updateSelection = updateSelection;
        this.selectionTarget = selectionTarget;
        this.preserveSurvivingSelection = preserveSurvivingSelection;
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

    /** Requests a refreshed roster that keeps the selected profile only if it still exists. */
    public static CommandResult forSurvivingSelection(String feedback) {
        return new CommandResult(feedback, false, false, true, null, true);
    }

    public Person getSelectionTarget() {
        return selectionTarget;
    }

    public boolean isPreserveSurvivingSelection() {
        return preserveSurvivingSelection;
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
                && preserveSurvivingSelection == otherCommandResult.preserveSurvivingSelection;
    }

    @Override
    public int hashCode() {
        return Objects.hash(feedbackToUser, showHelp, exit, updateSelection, selectionTarget,
                preserveSurvivingSelection);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("feedbackToUser", feedbackToUser)
                .add("showHelp", showHelp)
                .add("exit", exit)
                .add("updateSelection", updateSelection)
                .add("preserveSurvivingSelection", preserveSurvivingSelection)
                .toString();
    }

}
