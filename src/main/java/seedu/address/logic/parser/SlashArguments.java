package seedu.address.logic.parser;

import java.util.Map;
import java.util.Optional;

import seedu.address.logic.parser.exceptions.ParseException;

/** Validated prefix occurrences; values are checked in each command's documented order. */
public final class SlashArguments {
    private final Map<Prefix, String> values;
    private final String usage;

    SlashArguments(Map<Prefix, String> values, String usage) {
        this.values = Map.copyOf(values);
        this.usage = usage;
    }

    /** Returns a required non-empty value, or a syntax error with usage. */
    public String getRequired(Prefix prefix) throws ParseException {
        if (!values.containsKey(prefix)) {
            throw error("Missing required parameter: " + prefix);
        }
        return getOptional(prefix).orElseThrow();
    }

    /** Returns an optional non-empty value; omission is distinct from a supplied empty value. */
    public Optional<String> getOptional(Prefix prefix) throws ParseException {
        String value = values.get(prefix);
        if (value != null && value.isEmpty()) {
            throw error("A value is required for " + prefix + ".");
        }
        return Optional.ofNullable(value);
    }

    private ParseException error(String message) {
        return new ParseException(message + "\nUsage: " + usage);
    }
}
