package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Comparator;
import java.util.Locale;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.model.Model;
import seedu.address.model.person.NameOrEmailContainsQueryPredicate;
import seedu.address.model.person.Person;

/**
 * Finds students whose name or email contains a single literal query, ignoring case.
 */
public class FindCommand extends Command {
    public static final String COMMAND_WORD = "find";
    public static final String MESSAGE_USAGE = COMMAND_WORD
            + ": Searches names and emails for one literal substring, ignoring case.\n"
            + "Parameters: QUERY (1 to 100 Unicode characters)\n"
            + "Example: " + COMMAND_WORD + " Alex Tan\n"
            + "Telegram and GitHub searches are not available in v1.2.";
    public static final String MESSAGE_EMPTY_QUERY = "Enter a name or email to search.";
    public static final String MESSAGE_LONG_QUERY = "Search text must not contain more than 100 characters.";

    private static final Comparator<Person> RESULT_ORDER = Comparator
            .comparing((Person person) -> person.getName().fullName.toLowerCase(Locale.ROOT))
            .thenComparing(person -> person.getEmail().value.toLowerCase(Locale.ROOT));

    private final String query;

    /**
     * Creates a search for a validated, whitespace-normalized query.
     */
    public FindCommand(String query) {
        this.query = requireNonNull(query);
    }

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.updateFilteredPersonList(new NameOrEmailContainsQueryPredicate(query), RESULT_ORDER);
        return CommandResult.forSearch(Messages.formatSearchResult(query, model.getFilteredPersonList().size()));
    }

    @Override
    public boolean isReadOnly() {
        return true;
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
