package seedu.address.logic;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import seedu.address.logic.parser.Prefix;
import seedu.address.model.person.Person;

/**
 * Container for user visible messages.
 */
public class Messages {

    public static final String MESSAGE_LOAD_FAILURE = "Stored data could not be loaded. "
            + "The existing file was preserved. "
            + "This session is read-only. Restore a valid data file and restart SoCdex.";
    public static final String MESSAGE_EMPTY_ROSTER =
            "No students in the roster. Add a student or load fictional samples.";


    public static final String MESSAGE_SEARCH_DISPLAY_FAILURE =
            "Search results could not be displayed. Try the search again.";
    public static final String MESSAGE_PROFILE_DISPLAY_FAILURE =
            "Student could not be displayed. No data was changed. Try again.";
    public static final String MESSAGE_SINGLE_LINE = "Enter one command on a single line.";
    public static final String MESSAGE_ENTER_COMMAND = "Enter a command.";
    public static final String MESSAGE_PENDING_DELETION_CANCELLED = "Pending deletion cancelled.";
    public static final String MESSAGE_UNKNOWN_COMMAND =
            "Unknown command. Check the command name and use lowercase command words.";
    public static final String MESSAGE_INVALID_COMMAND_FORMAT = "Invalid command format!\n%1$s";
    public static final String MESSAGE_INVALID_PERSON_DISPLAYED_INDEX = "The person index provided is invalid.";
    public static final String MESSAGE_PERSONS_LISTED_OVERVIEW = "%1$d person(s) listed!";
    public static final String MESSAGE_DUPLICATE_FIELDS =
                "Multiple values specified for the following single-valued field(s): ";

    /**
     * Formats the number of matching students and the normalized, case-preserved query.
     */
    public static String formatSearchResult(String query, int count) {
        if (count == 0) {
            return "No students found for \"" + query + "\". Check the spelling or search with another identifier.";
        }
        return count + (count == 1 ? " student" : " students") + " found for \"" + query + "\".";
    }

    /**
     * Returns an error message indicating the duplicate prefixes.
     */
    public static String getErrorMessageForDuplicatePrefixes(Prefix... duplicatePrefixes) {
        assert duplicatePrefixes.length > 0;

        Set<String> duplicateFields =
                Stream.of(duplicatePrefixes).map(Prefix::toString).collect(Collectors.toSet());

        return MESSAGE_DUPLICATE_FIELDS + String.join(" ", duplicateFields);
    }

    /**
     * Formats the {@code person} for display to the user.
     */
    public static String format(Person person) {
        final StringBuilder builder = new StringBuilder();
        builder.append(person.getName())
                .append("; Email: ")
                .append(person.getEmail())
                .append("; Tags: ");
        person.getTags().forEach(builder::append);
        return builder.toString();
    }

}
