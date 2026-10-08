package seedu.address.logic.parser;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import seedu.address.logic.parser.exceptions.ParseException;

/** Tokenizes slash prefixes at space/tab boundaries, retaining structural error order. */
public final class SlashPrefixTokenizer {
    private static final Pattern SLASH_TOKEN = Pattern.compile("(?<![^ \t])/[^ \t]*");
    private static final String MALFORMED =
            "Invalid parameter syntax. Use the parameter prefixes shown in the command format.";

    private SlashPrefixTokenizer() {
    }

    /**
     * Rejects unexpected preamble, malformed, unknown and repeated prefixes from left to right.
     * Values terminate only at boundary prefixes, so an embedded slash such as AY26/27 is retained.
     */
    public static SlashArguments tokenize(String args, String usage, Prefix... allowedPrefixes) throws ParseException {
        ParserUtil.requireSingleLine(args);
        Set<String> allowed = Arrays.stream(allowedPrefixes).map(Prefix::getPrefix).collect(Collectors.toSet());
        Map<Prefix, String> values = new LinkedHashMap<>();
        Matcher matcher = SLASH_TOKEN.matcher(args);
        Prefix previous = null;
        int valueStart = 0;
        while (matcher.find()) {
            if (previous == null && !strip(args.substring(0, matcher.start())).isEmpty()) {
                throw error("Unexpected text before the first parameter.", usage);
            }
            String token = matcher.group();
            if (!token.matches("/[A-Za-z][A-Za-z-]*")) {
                throw error(MALFORMED, usage);
            }
            if (!allowed.contains(token)) {
                throw error("Unknown parameter: " + token, usage);
            }
            Prefix current = new Prefix(token);
            if (values.containsKey(current)) {
                throw error("Parameter " + token + " can be supplied only once.", usage);
            }
            if (previous != null) {
                values.put(previous, strip(args.substring(valueStart, matcher.start())));
            }
            values.put(current, "");
            previous = current;
            valueStart = matcher.end();
        }
        if (previous == null) {
            if (!strip(args).isEmpty()) {
                throw error("Unexpected text before the first parameter.", usage);
            }
        } else {
            values.put(previous, strip(args.substring(valueStart)));
        }
        return new SlashArguments(values, usage);
    }

    private static String strip(String value) {
        return value.replaceAll("\\A[ \\t]+|[ \\t]+\\z", "");
    }

    private static ParseException error(String message, String usage) {
        return new ParseException(message + "\nUsage: " + usage);
    }
}
