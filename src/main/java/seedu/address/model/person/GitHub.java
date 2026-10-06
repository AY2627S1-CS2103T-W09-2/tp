package seedu.address.model.person;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

/**
 * Represents a student's GitHub username.
 * Guarantees: immutable; is valid as declared in {@link #isValidGitHub(String)}
 */
public class GitHub {

    public static final String MESSAGE_CONSTRAINTS = "GitHub username must contain 1 to 39 letters, digits, "
            + "or hyphens. It must not start or end with a hyphen or contain consecutive hyphens.";

    /*
     * Runs of ASCII letters or digits joined by single hyphens.
     */
    public static final String VALIDATION_REGEX = "[A-Za-z0-9]+(-[A-Za-z0-9]+)*";
    public static final int MAX_LENGTH = 39;

    private static final String SURROUNDING_SPACES_AND_TABS_REGEX = "\\A[ \\t]+|[ \\t]+\\z";

    /** The username without surrounding spaces or tabs, in its entered case. */
    public final String value;

    /**
     * Constructs a {@code GitHub}.
     *
     * @param username A valid GitHub username, optionally with surrounding spaces or tabs.
     */
    public GitHub(String username) {
        requireNonNull(username);
        checkArgument(isValidGitHub(username), MESSAGE_CONSTRAINTS);
        value = normalise(username);
    }

    /**
     * Returns true if a given string is a valid GitHub username.
     */
    public static boolean isValidGitHub(String test) {
        requireNonNull(test);
        String username = normalise(test);
        return username.length() <= MAX_LENGTH && username.matches(VALIDATION_REGEX);
    }

    /**
     * Removes surrounding spaces and tabs.
     */
    private static String normalise(String username) {
        return username.replaceAll(SURROUNDING_SPACES_AND_TABS_REGEX, "");
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
        if (!(other instanceof GitHub otherGitHub)) {
            return false;
        }

        return value.equals(otherGitHub.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

}
