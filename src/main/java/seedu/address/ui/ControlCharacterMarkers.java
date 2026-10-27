package seedu.address.ui;

import java.util.BitSet;

/**
 * Tracks the markers that the command box shows in place of control characters. A text field silently drops control
 * characters, so two pasted lines would otherwise run together into a different command. Each line break is shown
 * as {@link #LINE_BREAK_MARKER}, each other control character as {@link #CONTROL_MARKER}, and a tab as a space.
 * Only markers inserted here are tracked, so the same symbols typed as ordinary text remain ordinary text.
 */
class ControlCharacterMarkers {
    static final String LINE_BREAK_MARKER = "↵";
    static final String CONTROL_MARKER = "�";

    /** Positions in the field text that hold a marker for a removed control character. */
    private BitSet markers = new BitSet();
    /** The field text, and its markers, expected after the change last passed to {@link #toShownText}. */
    private String expectedText;
    private BitSet expectedMarkers;

    /**
     * Returns {@code inserted} as the field should show it when it replaces {@code fieldText} from {@code start} to
     * {@code end}, and records where its markers will be.
     */
    String toShownText(String fieldText, int start, int end, String inserted) {
        StringBuilder shown = new StringBuilder();
        BitSet insertedMarkers = new BitSet();
        for (int i = 0; i < inserted.length(); i++) {
            char c = inserted.charAt(i);
            if (c == '\t') {
                shown.append(' ');
            } else if (c == '\r' || c == '\n') {
                if (c == '\r' && i + 1 < inserted.length() && inserted.charAt(i + 1) == '\n') {
                    i++;
                }
                insertedMarkers.set(shown.length());
                shown.append(LINE_BREAK_MARKER);
            } else if (c < ' ' || c == '\u007f') {
                insertedMarkers.set(shown.length());
                shown.append(CONTROL_MARKER);
            } else {
                shown.append(c);
            }
        }
        expectedText = fieldText.substring(0, start) + shown + fieldText.substring(end);
        expectedMarkers = splice(markers, start, end, insertedMarkers, shown.length());
        return shown.toString();
    }

    /**
     * Updates the marker positions after the field text changes. A change that did not pass through
     * {@link #toShownText}, such as an undo, may restore a marker; any marker symbol it inserts is treated as a
     * removed control character, so the command is rejected rather than silently changed.
     */
    void textChanged(String oldText, String newText) {
        if (newText.equals(expectedText)) {
            markers = expectedMarkers;
        } else {
            int shorter = Math.min(oldText.length(), newText.length());
            int prefix = 0;
            while (prefix < shorter && oldText.charAt(prefix) == newText.charAt(prefix)) {
                prefix++;
            }
            int suffix = 0;
            while (suffix < shorter - prefix && oldText.charAt(oldText.length() - 1 - suffix)
                    == newText.charAt(newText.length() - 1 - suffix)) {
                suffix++;
            }
            int insertedLength = newText.length() - suffix - prefix;
            BitSet restored = new BitSet();
            for (int i = 0; i < insertedLength; i++) {
                if (isMarker(newText.charAt(prefix + i))) {
                    restored.set(i);
                }
            }
            markers = splice(markers, prefix, oldText.length() - suffix, restored, insertedLength);
        }
        expectedText = null;
        expectedMarkers = null;
    }

    /** Returns the command to submit for {@code fieldText}, with a line break at each tracked marker. */
    String toCommandText(String fieldText) {
        StringBuilder command = new StringBuilder(fieldText);
        markers.stream().filter(i -> i < command.length()).forEach(i -> command.setCharAt(i, '\n'));
        return command.toString();
    }

    private static boolean isMarker(char c) {
        return LINE_BREAK_MARKER.charAt(0) == c || CONTROL_MARKER.charAt(0) == c;
    }

    /** Returns {@code bits} with positions {@code start} to {@code end} replaced by {@code inserted}. */
    private static BitSet splice(BitSet bits, int start, int end, BitSet inserted, int insertedLength) {
        BitSet result = bits.get(0, start);
        inserted.stream().forEach(i -> result.set(start + i));
        bits.get(end, Math.max(end, bits.length())).stream().forEach(i -> result.set(start + insertedLength + i));
        return result;
    }
}
