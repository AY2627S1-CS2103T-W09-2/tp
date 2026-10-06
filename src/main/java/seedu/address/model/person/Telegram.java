package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * Represents a student's Telegram handle.
 * Guarantees: immutable; is valid as declared in {@link #isValidTelegram(String)}
 */
public class Telegram {

    public static final String MESSAGE_CONSTRAINTS = "Telegram handle must contain 5 to 32 letters, digits, "
            + "or underscores and must start with a letter. One leading @ is allowed.";

    /*
     * An ASCII letter followed by 4 to 31 ASCII letters, digits or underscores.
     */
    public static final String VALIDATION_REGEX = "[A-Za-z][A-Za-z0-9_]{4,31}";

    private static final String SURROUNDING_SPACES_AND_TABS_REGEX = "\\A[ \\t]+|[ \\t]+\\z";
    private static final String OPTIONAL_PREFIX = "@";

    /** The handle without surrounding spaces or tabs or its optional leading {@code @}, in its entered case. */
    public final String value;

    /**
     * Constructs a {@code Telegram}.
     *
     * @param handle A valid Telegram handle, optionally with surrounding spaces or tabs and one leading {@code @}.
     */
    public Telegram(String handle) {
        requireNonNull(handle);
        checkArgument(isValidTelegram(handle), MESSAGE_CONSTRAINTS);
        value = normalise(handle);
    }

    /**
     * Returns true if a given string is a valid Telegram handle.
     */
    public static boolean isValidTelegram(String test) {
        requireNonNull(test);
        return normalise(test).matches(VALIDATION_REGEX);
    }

    /**
     * Removes surrounding spaces and tabs, then one leading {@code @}.
     */
    private static String normalise(String handle) {
        String stripped = handle.replaceAll(SURROUNDING_SPACES_AND_TABS_REGEX, "");
        return stripped.startsWith(OPTIONAL_PREFIX) ? stripped.substring(OPTIONAL_PREFIX.length()) : stripped;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Telegram otherTelegram)) {
            return false;
        }

        return value.equals(otherTelegram.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
