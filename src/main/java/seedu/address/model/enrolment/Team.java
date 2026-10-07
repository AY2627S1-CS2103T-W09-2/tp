package seedu.address.model.enrolment;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;


/** Represents an immutable, validated team value for one enrolment. */
public final class Team {
    public static final String MESSAGE_CONSTRAINTS =
            "Team must contain 1 to 30 letters, digits, spaces, or hyphens and include at least one letter "
            + "or digit.";

    public final String value;

    /** Creates a validated value from command input, applying the specified normalisation. */
    public Team(String input) {
        requireNonNull(input);
        checkArgument(isValidTeam(input), MESSAGE_CONSTRAINTS);
        value = normalise(input);
    }

    /** Returns whether the input satisfies the field rules after normalisation. */
    public static boolean isValidTeam(String input) {
        requireNonNull(input);
        String normalised = normalise(input);
        return normalised.matches("[A-Za-z0-9 -]{1,30}") && normalised.matches(".*[A-Za-z0-9].*");
    }

    private static String normalise(String input) {
        return EnrolmentValues.normaliseSpaces(input);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Team otherValue && value.equals(otherValue.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
