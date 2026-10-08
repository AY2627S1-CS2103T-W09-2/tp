package seedu.address.model.person;

import java.util.Comparator;
import java.util.Locale;

/** The shared display order for searches and the full roster. */
public final class PersonOrder {
    public static final Comparator<Person> BY_NAME_THEN_EMAIL = Comparator
            .comparing((Person person) -> person.getName().fullName.toLowerCase(Locale.ROOT))
            .thenComparing(person -> person.getEmail().value);

    private PersonOrder() {
    }
}
