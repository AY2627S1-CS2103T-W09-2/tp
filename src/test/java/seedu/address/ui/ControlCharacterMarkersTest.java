package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.Messages.MESSAGE_SINGLE_LINE;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.commands.SampleCommand;
import seedu.address.logic.parser.AddressBookParser;
import seedu.address.logic.parser.exceptions.ParseException;

public class ControlCharacterMarkersTest {
    private static final String LINE = ControlCharacterMarkers.LINE_BREAK_MARKER;
    private static final String CONTROL = ControlCharacterMarkers.CONTROL_MARKER;

    private final ControlCharacterMarkers markers = new ControlCharacterMarkers();
    private String field = "";

    @Test
    public void toShownText_tab_becomesSpace() {
        assertEquals("sample load", insert(0, 0, "sample\tload"));
        assertEquals("sample load", markers.toCommandText(field));
    }

    @Test
    public void toShownText_lineBreaks_becomeOneMarkerEach() {
        assertEquals("a" + LINE + "b" + LINE + "c" + LINE + LINE + "d", insert(0, 0, "a\nb\r\nc\r\rd"));
        assertEquals("a\nb\nc\n\nd", markers.toCommandText(field));
    }

    @Test
    public void toShownText_otherControlCharacters_becomeControlMarker() {
        assertEquals("sample" + CONTROL + "load" + CONTROL, insert(0, 0, "sample\u000bload\u007f"));
        assertEquals("sample\nload\n", markers.toCommandText(field));
    }

    @Test
    public void toCommandText_typedMarkerSymbols_keptAsText() {
        insert(0, 0, "find A" + LINE + "B" + CONTROL);
        assertEquals("find A" + LINE + "B" + CONTROL, markers.toCommandText(field));
    }

    @Test
    public void toCommandText_editsAroundMarker_followMarker() {
        insert(0, 0, "find Alex\nview e1");
        insert(0, 0, "  ");
        assertEquals("  find Alex\nview e1", markers.toCommandText(field));
        insert(2, 7, "");
        assertEquals("  Alex\nview e1", markers.toCommandText(field));
        insert(field.indexOf(LINE), field.indexOf(LINE) + 1, " ");
        assertEquals("  Alex view e1", markers.toCommandText(field));
    }

    @Test
    public void textChanged_unfilteredRestore_treatsRestoredMarkerAsLineBreak() {
        insert(0, 0, "a\nb");
        insert(1, 2, "");
        String restored = "a" + LINE + "b";
        markers.textChanged(field, restored);
        field = restored;
        assertEquals("a\nb", markers.toCommandText(field));
    }

    @Test
    public void toCommandText_pastedTab_parsesAsSampleLoad() throws Exception {
        insert(0, 0, "sample\tload");
        assertEquals(SampleCommand.class, new AddressBookParser().parseCommand(markers.toCommandText(field))
                .getClass());
    }

    @Test
    public void toCommandText_pastedLineBreak_rejectedAsMultiLine() {
        insert(0, 0, "find Alex\r\nview e9000001@u.nus.edu");
        String command = markers.toCommandText(field);
        assertThrows(ParseException.class, MESSAGE_SINGLE_LINE, () -> new AddressBookParser().parseCommand(command));
    }

    @Test
    public void toCommandText_typedMarkerSymbol_parsesAsLiteralSearch() throws Exception {
        insert(0, 0, "find " + LINE);
        assertEquals(FindCommand.class, new AddressBookParser().parseCommand(markers.toCommandText(field))
                .getClass());
    }

    /** Applies an edit as the command box's text formatter and text listener do, and returns the shown text. */
    private String insert(int start, int end, String inserted) {
        String shown = markers.toShownText(field, start, end, inserted);
        String newField = field.substring(0, start) + shown + field.substring(end);
        markers.textChanged(field, newField);
        field = newField;
        return shown;
    }
}
