package seedu.address.model.enrolment;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import java.util.Locale;

/** Represents an immutable, validated module code value for one enrolment. */
public final class ModuleCode {
    public static final String MESSAGE_CONSTRAINTS =
            "Module code must contain 2 to 4 letters, four digits, and up to 3 final letters, such as "
            + "CS2103T.";

    public final String value;

    /** Creates a validated value from command input, applying the specified normalisation. */
    public ModuleCode(String input) {
        requireNonNull(input);
        checkArgument(isValidModuleCode(input), MESSAGE_CONSTRAINTS);
        value = normalise(input);
    }

    /** Returns whether the input satisfies the field rules after normalisation. */
    public static boolean isValidModuleCode(String input) {
        requireNonNull(input);
        String normalised = EnrolmentValues.trim(input);
        return normalised.matches("[A-Za-z]{2,4}[0-9]{4}[A-Za-z]{0,3}");
    }

    private static String normalise(String input) {
        return EnrolmentValues.trim(input).toUpperCase(Locale.ROOT);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ModuleCode otherValue && value.equals(otherValue.value);
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
