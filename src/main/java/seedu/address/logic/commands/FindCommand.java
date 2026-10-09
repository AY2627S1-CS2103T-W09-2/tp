package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.model.Model;
import seedu.address.model.person.IdentifierContainsQueryPredicate;
import seedu.address.model.person.PersonOrder;

/**
 * Finds students by a literal substring in their name, email, Telegram or GitHub, ignoring case.
 */
public class FindCommand extends Command {
    public static final String COMMAND_WORD = "find";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Searches names, NUS emails, Telegram handles and GitHub usernames "
            + "for one literal substring, ignoring case.\n"
            + "Parameters: QUERY (1 to 100 Unicode characters)\n"
            + "Examples: " + COMMAND_WORD + " Alex Tan; " + COMMAND_WORD + " @socdex_demo_mei\n"
            + "One leading @ is ignored for Telegram comparison only.";
    public static final String MESSAGE_EMPTY_QUERY =
            "Enter a name, NUS email, Telegram handle, or GitHub username to search.";
    public static final String MESSAGE_LONG_QUERY = "Search text must not contain more than 100 characters.";

    private final String query;

    /**
     * Creates a search for a validated, whitespace-normalized query.
     */
    public FindCommand(String query) {
        this.query = requireNonNull(query);
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.updateFilteredPersonList(new IdentifierContainsQueryPredicate(query), PersonOrder.BY_NAME_THEN_EMAIL);
        return CommandResult.forSearch(Messages.formatSearchResult(query, model.getFilteredPersonList().size()));
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof FindCommand otherCommand && query.equals(otherCommand.query);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("query", query).toString();
    }
}
