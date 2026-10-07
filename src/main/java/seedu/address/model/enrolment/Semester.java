package seedu.address.model.enrolment;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import java.util.Locale;

/** Represents an immutable, validated semester value for one enrolment. */
public final class Semester {
    public static final String MESSAGE_CONSTRAINTS =
            "Semester must use AYyy/yy S1 or AYyy/yy S2 with consecutive years from 2000 to 2099, such as "
            + "AY26/27 S1.";

    public final String value;

    /** Creates a validated value from command input, applying the specified normalisation. */
    public Semester(String input) {
        requireNonNull(input);
        checkArgument(isValidSemester(input), MESSAGE_CONSTRAINTS);
        value = normalise(input);
    }

    /** Returns whether the input satisfies the field rules after normalisation. */
    public static boolean isValidSemester(String input) {
        requireNonNull(input);
        String normalised = EnrolmentValues.normaliseSpaces(input);
        return normalised.matches("(?i:AY)[0-9]{2}/[0-9]{2} (?i:S)[12]")
                && Integer.parseInt(normalised.substring(5, 7)) == Integer.parseInt(normalised.substring(2, 4)) + 1;
    }

    private static String normalise(String input) {
        return EnrolmentValues.normaliseSpaces(input).toUpperCase(Locale.ROOT);
    }

    /** Returns the academic start year within 2000 to 2099. */
    public int getStartYear() {
        return 2000 + Integer.parseInt(value.substring(2, 4));
    }

    /** Returns semester 1 or 2. */
    public int getSemesterNumber() {
        return value.charAt(value.length() - 1) - '0';
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Semester otherValue && value.equals(otherValue.value);
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
