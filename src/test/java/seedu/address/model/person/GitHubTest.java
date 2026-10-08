package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class GitHubTest {

    private static final String[] VALID_USERNAMES = {
        "x", "0", "A1", // 1 or 2 characters
        "a".repeat(39), // 39 characters, the maximum
        "a-".repeat(19) + "a", // 39 characters with single internal hyphens
        "alex-tan", "Alex-Tan-2", // internal hyphens
        "clear", // an ordinary username in this type
        " \tAlex-Tan\t " // surrounding spaces and tabs
    };

    private static final String[] INVALID_USERNAMES = {
        "", " ", "\t", // blank, 0 characters after stripping
        "a".repeat(40), "a-".repeat(19) + "ab", // 40 characters
        "-", "-alex", "alex-", // leading or trailing hyphen
        "alex--tan", // consecutive hyphens
        "alex_tan", "alex_", // underscores
        "@alex", // leading @
        "https://github.com/alex", "github.com/alex", "alex/tan", "alex.tan", // URLs and other punctuation
        "alex tan", "alex\ttan", // internal whitespace
        "alex\n", "\nalex", "alex\r", "\ralex", // line breaks are not stripped
        "\u0000alex", "alex\u0001", "alex\u007F", "alex\u2028", // unsupported controls
        "\u00E9lise", "al\u00E9x", // non-ASCII letters (e with acute accent)
        "\u00A0alex", "alex\u00A0", // no-break space is not stripped
        "\u212Aevin" // KELVIN SIGN is not an ASCII letter
    };

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new GitHub(null));
    }

    @Test
    public void constructor_invalidUsername_throwsIllegalArgumentException() {
        for (String username : INVALID_USERNAMES) {
            assertThrows(IllegalArgumentException.class, GitHub.MESSAGE_CONSTRAINTS, () -> new GitHub(username));
        }
    }

    @Test
    public void constructor_validUsername_storesNormalisedValueInEnteredCase() {
        assertEquals("Alex-Tan", new GitHub(" \tAlex-Tan\t ").value);
        assertEquals("ALEX-TAN", new GitHub("ALEX-TAN").value);
        assertEquals("alex-tan", new GitHub("alex-tan").value);
        assertEquals("clear", new GitHub("clear").value);
    }

    @Test
    public void isValidGitHub() {
        // null username
        assertThrows(NullPointerException.class, () -> GitHub.isValidGitHub(null));

        // the constructor accepts every valid username, and its stored value remains valid
        for (String username : VALID_USERNAMES) {
            assertTrue(GitHub.isValidGitHub(username), username);
            assertTrue(GitHub.isValidGitHub(new GitHub(username).value), username);
        }

        for (String username : INVALID_USERNAMES) {
            assertFalse(GitHub.isValidGitHub(username), username);
        }
    }

    @Test
    public void equals() {
        GitHub gitHub = new GitHub("Alex-Tan");

        // same normalised value -> returns true, with equal hash codes
        GitHub paddedGitHub = new GitHub(" Alex-Tan\t");
        assertTrue(gitHub.equals(paddedGitHub));
        assertEquals(gitHub.hashCode(), paddedGitHub.hashCode());

        // same object -> returns true
        assertTrue(gitHub.equals(gitHub));

        // null -> returns false
        assertFalse(gitHub.equals(null));

        // different types -> returns false
        assertFalse(gitHub.equals(5.0f));

        // different case -> returns false, so a case-only change remains detectable
        assertFalse(gitHub.equals(new GitHub("alex-tan")));

        // different values -> returns false
        assertFalse(gitHub.equals(new GitHub("mei-lim")));
    }

    @Test
    public void toStringMethod() {
        assertEquals("Alex-Tan", new GitHub(" Alex-Tan ").toString());
    }
}
