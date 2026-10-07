package seedu.address.model.enrolment;

/** Normalises the spaces and tabs permitted in enrolment input, without accepting line breaks. */
final class EnrolmentValues {
    private EnrolmentValues() {
    }

    static String trim(String input) {
        return input.replaceAll("^[ \t]+|[ \t]+$", "");
    }

    static String normaliseSpaces(String input) {
        return trim(input).replaceAll("[ \t]+", " ");
    }
}
