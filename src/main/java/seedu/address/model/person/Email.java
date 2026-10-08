package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import java.util.Locale;

/**
 * Represents a student's NUS email, which identifies their profile.
 * The stored value is canonical: surrounding spaces and tabs are removed and letters are lowercase.
 * Validation is a local syntax check only; it does not verify that an institutional account exists.
 * Guarantees: immutable; is valid as declared in {@link #isValidEmail(String)}
 */
public class Email {

    public static final String MESSAGE_CONSTRAINTS = "Email must use a valid local part followed by @u.nus.edu. "
            + "Use letters, digits, dots, underscores, plus signs, or hyphens in the local part, "
            + "with no spaces or consecutive dots. Start and end the local part with a letter or digit.";

    // Applies to the canonical (lowercase) form: a local part of 1 to 64 permitted characters that starts and ends
    // with a letter or digit, followed by exactly "@u.nus.edu". Consecutive dots are rejected separately.
    private static final String LOCAL_PART_REGEX = "[a-z0-9]([a-z0-9._+-]{0,62}[a-z0-9])?";
    public static final String VALIDATION_REGEX = LOCAL_PART_REGEX + "@u\\.nus\\.edu";

    private static final String SURROUNDING_SPACES_AND_TABS_REGEX = "^[ \\t]+|[ \\t]+$";
    private static final char LAST_ASCII_CHARACTER = 127;

    public final String value;

    /**
     * Constructs an {@code Email} holding the canonical form of {@code email}.
     *
     * @param email A valid email address, possibly with surrounding spaces or tabs and uppercase letters.
     */
    public Email(String email) {
        requireNonNull(email);
        checkArgument(isValidEmail(email), MESSAGE_CONSTRAINTS);
        value = toCanonical(email);
    }

    /**
     * Returns true if a given string is a valid NUS email after its canonicalisation.
     * Only surrounding spaces and tabs are ignored; any other character, including line breaks, control
     * characters and non-ASCII characters, makes the email invalid.
     */
    public static boolean isValidEmail(String test) {
        requireNonNull(test);
        String stripped = stripSpacesAndTabs(test);
        // Checked before lowercasing so that a non-ASCII character cannot lowercase into an ASCII one.
        if (!isAscii(stripped)) {
            return false;
        }
        String canonical = stripped.toLowerCase(Locale.ROOT);
        return canonical.matches(VALIDATION_REGEX) && !canonical.contains("..");
    }

    private static String toCanonical(String email) {
        return stripSpacesAndTabs(email).toLowerCase(Locale.ROOT);
    }

    private static String stripSpacesAndTabs(String text) {
        return text.replaceAll(SURROUNDING_SPACES_AND_TABS_REGEX, "");
    }

    private static boolean isAscii(String text) {
        return text.chars().allMatch(character -> character <= LAST_ASCII_CHARACTER);
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
        if (!(other instanceof Email otherEmail)) {
            return false;
        }

        return value.equals(otherEmail.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
