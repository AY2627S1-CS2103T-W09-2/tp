package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class NameTest {

    private static final String SUPPLEMENTARY = "\uD840\uDC00"; // U+20000, one code point stored in two chars
    private static final String HIGH_SURROGATE = "\uD840";
    private static final String LOW_SURROGATE = "\uDC00";

    private static final String[] VALID_NAMES = {
        "Alex Tan",
        "^", "peter*", "R@chel", "O'Brien", // punctuation other than a forward slash
        "Jos\u00E9 Tan", "\u738B\u5C0F\u660E", // accented and non-Latin names
        "a".repeat(100), // 100 code points, the maximum
        SUPPLEMENTARY, // a supplementary character on its own
        SUPPLEMENTARY.repeat(100), // 100 code points stored in 200 chars
        "a".repeat(99) + SUPPLEMENTARY, // 100 code points
        " \t" + "a".repeat(100) + "\t ", // surrounding spaces and tabs are not counted
        "Alex\u00A0Tan", // a no-break space is kept as entered
        "clear" // an ordinary name
    };

    private static final String[] INVALID_NAMES = {
        "", " ", "\t", " \t ", // blank
        "/", "Alex/Tan", "Alex /Tan", // forward slash
        "a".repeat(101), SUPPLEMENTARY.repeat(101), "a".repeat(100) + SUPPLEMENTARY, // 101 code points
        "a".repeat(100) + " b", // 102 code points after normalisation
        "Alex\nTan", "\nAlex", "Alex\r", "Alex\u2028Tan", "Alex\u2029", // line breaks
        "Alex\u0000", "Alex\u0001", "Alex\u000B", "Alex\u007F", "Alex\u0085", // control characters
        "\u00A0", "\u3000", "\u200B", "\u200B\u00A0", "\uFEFF", // no visible character
        HIGH_SURROGATE, LOW_SURROGATE, // isolated surrogates
        "Alex" + HIGH_SURROGATE + "Tan", "Alex" + LOW_SURROGATE, // isolated surrogate in a visible name
        LOW_SURROGATE + HIGH_SURROGATE, "Alex" + LOW_SURROGATE + HIGH_SURROGATE, // reversed pair
        HIGH_SURROGATE + "Alex", "Alex" + HIGH_SURROGATE, HIGH_SURROGATE + SUPPLEMENTARY // broken pairs
    };

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Name(null));
    }

    @Test
    public void constructor_invalidName_throwsIllegalArgumentException() {
        for (String invalidName : INVALID_NAMES) {
            assertThrows(IllegalArgumentException.class, Name.MESSAGE_CONSTRAINTS, () -> new Name(invalidName));
        }
    }

    @Test
    public void constructor_validName_storesNormalisedName() {
        assertEquals("Alex Tan", new Name(" \tAlex \t Tan\t ").fullName);
        assertEquals("aLEX", new Name("aLEX").fullName); // case is preserved
        assertEquals("Alex\u00A0Tan", new Name("Alex\u00A0Tan").fullName); // only spaces and tabs are normalised
    }

    @Test
    public void constructor_repeatedSpaces_lengthMeasuredAfterNormalisation() {
        String raw = ("a" + " ".repeat(2)).repeat(49) + "a";
        assertEquals(148, raw.length());

        Name name = new Name(raw);
        assertEquals("a ".repeat(49) + "a", name.fullName);
        assertEquals(99, name.fullName.codePointCount(0, name.fullName.length()));
    }

    @Test
    public void isValidName() {
        // null name
        assertThrows(NullPointerException.class, () -> Name.isValidName(null));

        // the constructor accepts every valid name, and its stored value remains valid
        for (String validName : VALID_NAMES) {
            assertTrue(Name.isValidName(validName), validName);
            assertTrue(Name.isValidName(new Name(validName).fullName), validName);
        }

        for (String invalidName : INVALID_NAMES) {
            assertFalse(Name.isValidName(invalidName), invalidName);
        }
    }

    @Test
    public void equals() {
        Name name = new Name("Valid Name");

        // same values -> returns true
        assertTrue(name.equals(new Name("Valid Name")));

        // same normalised value -> returns true, with equal hash codes
        Name spacedName = new Name(" Valid \t Name ");
        assertTrue(name.equals(spacedName));
        assertEquals(name.hashCode(), spacedName.hashCode());

        // same object -> returns true
        assertTrue(name.equals(name));

        // null -> returns false
        assertFalse(name.equals(null));

        // different types -> returns false
        assertFalse(name.equals(5.0f));

        // different case -> returns false
        assertFalse(name.equals(new Name("valid name")));

        // a no-break space is not normalised -> returns false
        assertFalse(name.equals(new Name("Valid\u00A0Name")));

        // different values -> returns false
        assertFalse(name.equals(new Name("Other Valid Name")));
    }

    @Test
    public void toStringMethod() {
        assertEquals("Alex Tan", new Name(" Alex  Tan ").toString());
    }
}
