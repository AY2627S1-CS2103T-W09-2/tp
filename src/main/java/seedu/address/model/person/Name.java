package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * Represents a Person's name in the address book.
 * Guarantees: immutable; is valid as declared in {@link #isValidName(String)}
 */
public class Name {

    public static final String MESSAGE_CONSTRAINTS =
            "Name must contain 1 to 100 characters and must not contain a forward slash or control character.";
    public static final int MAX_LENGTH = 100;

    private static final String SURROUNDING_SPACES_AND_TABS_REGEX = "\\A[ \\t]+|[ \\t]+\\z";
    private static final String SPACES_AND_TABS_REGEX = "[ \\t]+";
    private static final int FORWARD_SLASH = '/';
    private static final int LINE_SEPARATOR = 0x2028;
    private static final int PARAGRAPH_SEPARATOR = 0x2029;

    /** The name with surrounding spaces and tabs removed and each internal run of them replaced by one space. */
    public final String fullName;

    /**
     * Constructs a {@code Name}.
     *
     * @param name A valid name.
     */
    public Name(String name) {
        requireNonNull(name);
        checkArgument(isValidName(name), MESSAGE_CONSTRAINTS);
        fullName = normalise(name);
    }

    /**
     * Returns true if a given string is a valid name.
     */
    public static boolean isValidName(String test) {
        requireNonNull(test);
        String name = normalise(test);
        int length = name.codePointCount(0, name.length());
        return length >= 1 && length <= MAX_LENGTH
                && name.codePoints().noneMatch(Name::isProhibited)
                && name.codePoints().anyMatch(Name::isVisible);
    }

    /**
     * Removes surrounding spaces and tabs and replaces each internal run of them with one space.
     */
    private static String normalise(String name) {
        return name.replaceAll(SURROUNDING_SPACES_AND_TABS_REGEX, "").replaceAll(SPACES_AND_TABS_REGEX, " ");
    }

    /**
     * Returns true for a forward slash, a control character, a line break, or an unpaired surrogate.
     */
    private static boolean isProhibited(int codePoint) {
        return codePoint == FORWARD_SLASH || Character.isISOControl(codePoint)
                || codePoint == LINE_SEPARATOR || codePoint == PARAGRAPH_SEPARATOR
                || Character.getType(codePoint) == Character.SURROGATE;
    }

    /**
     * Returns true unless the code point is whitespace, a space separator or an invisible format character.
     */
    private static boolean isVisible(int codePoint) {
        return !Character.isWhitespace(codePoint) && !Character.isSpaceChar(codePoint)
                && Character.getType(codePoint) != Character.FORMAT;
    }


    @Override
    public String toString() {
        return fullName;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Name otherName)) {
            return false;
        }

        return fullName.equals(otherName.fullName);
    }

    @Override
    public int hashCode() {
        return fullName.hashCode();
    }

}
