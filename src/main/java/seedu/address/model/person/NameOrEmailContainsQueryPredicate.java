package seedu.address.model.person;

import static java.util.Objects.requireNonNull;

import java.util.Locale;
import java.util.function.Predicate;

/**
 * Tests whether a person's name or email contains one literal query, ignoring case.
 */
public class NameOrEmailContainsQueryPredicate implements Predicate<Person> {
    private final String lowercaseQuery;

    public NameOrEmailContainsQueryPredicate(String query) {
        lowercaseQuery = requireNonNull(query).toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean test(Person person) {
        return person.getName().fullName.toLowerCase(Locale.ROOT).contains(lowercaseQuery)
                || person.getEmail().value.toLowerCase(Locale.ROOT).contains(lowercaseQuery);
    }
}
