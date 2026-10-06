package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class TelegramTest {

    private static final String[] VALID_HANDLES = {
        "abcde", // 5 characters, the minimum
        "a" + "b".repeat(31), // 32 characters, the maximum
        "@abcde", // 5 characters after removing one leading @
        "@a" + "b".repeat(31), // 32 characters after removing one leading @
        "Alex_Tan", "ALEX_TAN", "alex_tan", "a1_2_3", "alex__tan_", // letters, digits and underscores
        "clear", // an ordinary handle in this type
        " \t@Alex_Tan\t " // surrounding spaces and tabs
    };

    private static final String[] INVALID_HANDLES = {
        "", " ", "\t", "@", " @ ", // blank
        "abcd", "@abcd", // 4 characters
        "a" + "b".repeat(32), "@a" + "b".repeat(32), // 33 characters
        "@@alex_tan", "@ alex_tan", "alex@tan", // a second or misplaced @
        "1alex_tan", "_alex_tan", // must start with a letter
        "alex tan", "alex\ttan", // internal whitespace
        "alex-tan", "alex.tan", "alex!tan", // other punctuation
        "alex_tan\n", "\nalex_tan", "alex_tan\r", "\ralex_tan", // line breaks are not stripped
        "\u0000alex_tan", "alex_tan\u0001", "alex_tan\u007F", "alex_tan\u2028", // unsupported controls
        "\u00E9lise_tan", "alex_t\u00E9n", // non-ASCII letters (e with acute accent)
        "\u00A0alex_tan", "alex_tan\u00A0", // no-break space is not stripped
        "\u212Aevin_tan" // KELVIN SIGN is not an ASCII letter
    };

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Telegram(null));
    }

    @Test
    public void constructor_invalidHandle_throwsIllegalArgumentException() {
        for (String handle : INVALID_HANDLES) {
            assertThrows(IllegalArgumentException.class, Telegram.MESSAGE_CONSTRAINTS, () -> new Telegram(handle));
        }
    }

    @Test
    public void constructor_validHandle_storesNormalisedValueInEnteredCase() {
        assertEquals("Alex_Tan", new Telegram("@Alex_Tan").value);
        assertEquals("Alex_Tan", new Telegram(" \tAlex_Tan\t ").value);
        assertEquals("ALEX_TAN", new Telegram("\t@ALEX_TAN ").value);
        assertEquals("alex_tan", new Telegram("alex_tan").value);
        assertEquals("clear", new Telegram("clear").value);
    }

    @Test
    public void isValidTelegram() {
        // null handle
        assertThrows(NullPointerException.class, () -> Telegram.isValidTelegram(null));

        // the constructor accepts every valid handle, and its stored value remains valid
        for (String handle : VALID_HANDLES) {
            assertTrue(Telegram.isValidTelegram(handle), handle);
            assertTrue(Telegram.isValidTelegram(new Telegram(handle).value), handle);
        }

        for (String handle : INVALID_HANDLES) {
            assertFalse(Telegram.isValidTelegram(handle), handle);
        }
    }

    @Test
    public void equals() {
        Telegram telegram = new Telegram("@Alex_Tan");

        // same normalised value -> returns true, with equal hash codes
        Telegram paddedTelegram = new Telegram(" Alex_Tan\t");
        assertTrue(telegram.equals(paddedTelegram));
        assertEquals(telegram.hashCode(), paddedTelegram.hashCode());

        // same object -> returns true
        assertTrue(telegram.equals(telegram));

        // null -> returns false
        assertFalse(telegram.equals(null));

        // different types -> returns false
        assertFalse(telegram.equals(5.0f));

        // different case -> returns false, so a case-only change remains detectable
        assertFalse(telegram.equals(new Telegram("alex_tan")));

        // different values -> returns false
        assertFalse(telegram.equals(new Telegram("mei_lim")));
    }

    @Test
    public void toStringMethod() {
        assertEquals("Alex_Tan", new Telegram(" @Alex_Tan ").toString());
    }
}
