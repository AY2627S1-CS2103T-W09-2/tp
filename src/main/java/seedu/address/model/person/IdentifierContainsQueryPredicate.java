package seedu.address.model.person;

import static java.util.Objects.requireNonNull;

import java.util.Locale;
import java.util.function.Predicate;

/** Tests whether any stored student identifier contains one literal query, ignoring case. */
public class IdentifierContainsQueryPredicate implements Predicate<Person> {
    private final String lowercaseQuery;
    private final String telegramQuery;

    /** Creates a predicate for a validated query, stripping one leading @ for Telegram only. */
    public IdentifierContainsQueryPredicate(String query) {
        lowercaseQuery = requireNonNull(query).toLowerCase(Locale.ROOT);
        telegramQuery = lowercaseQuery.startsWith("@") ? lowercaseQuery.substring(1) : lowercaseQuery;
    }

    @Override
    public boolean test(Person person) {
        return person.getName().fullName.toLowerCase(Locale.ROOT).contains(lowercaseQuery)
                || person.getEmail().value.toLowerCase(Locale.ROOT).contains(lowercaseQuery)
                || person.getTelegram().filter(handle -> !telegramQuery.isEmpty()
                    && handle.value.toLowerCase(Locale.ROOT).contains(telegramQuery)).isPresent()
                || person.getGitHub().filter(username ->
                    username.value.toLowerCase(Locale.ROOT).contains(lowercaseQuery)).isPresent();
    }
}
