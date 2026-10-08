package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class EmailTest {

    private static final String DOMAIN = "@u.nus.edu";

    @Test
    public void constructor_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Email(null));
    }

    @Test
    public void constructor_invalidEmail_throwsIllegalArgumentException() {
        String invalidEmail = "";
        assertThrows(IllegalArgumentException.class, () -> new Email(invalidEmail));
    }

    @Test
    public void constructor_validEmail_storesCanonicalForm() {
        assertEquals("e1234567@u.nus.edu", new Email("E1234567@U.NUS.EDU").value);
        assertEquals("e1234567@u.nus.edu", new Email(" \tE1234567@u.nus.edu\t ").value);
        assertEquals("kevin@u.nus.edu", new Email("Kevin@u.nus.edu").value);
        assertEquals("alex.tan+cs2103@u.nus.edu", new Email("Alex.Tan+CS2103@u.nus.edu").value);
    }

    @Test
    public void isValidEmail() {
        // null email
        assertThrows(NullPointerException.class, () -> Email.isValidEmail(null));

        // blank email
        assertFalse(Email.isValidEmail("")); // empty string
        assertFalse(Email.isValidEmail(" ")); // spaces only
        assertFalse(Email.isValidEmail("\t")); // tab only

        // missing parts
        assertFalse(Email.isValidEmail(DOMAIN)); // missing local part
        assertFalse(Email.isValidEmail("e1234567u.nus.edu")); // missing '@' symbol
        assertFalse(Email.isValidEmail("e1234567@")); // missing domain

        // unsupported domains
        assertFalse(Email.isValidEmail("e1234567@nus.edu.sg"));
        assertFalse(Email.isValidEmail("e1234567@u.nus.edu.sg"));
        assertFalse(Email.isValidEmail("e1234567@x.u.nus.edu")); // subdomain of the required domain
        assertFalse(Email.isValidEmail("e1234567@u-nus.edu"));
        assertFalse(Email.isValidEmail("e1234567@unus.edu"));
        assertFalse(Email.isValidEmail("peterjack@example.com")); // valid in AB3, unsupported here

        // extra '@'
        assertFalse(Email.isValidEmail("e1234567@@u.nus.edu"));
        assertFalse(Email.isValidEmail("e123@4567@u.nus.edu"));

        // disallowed characters in the local part
        assertFalse(Email.isValidEmail("e123 4567" + DOMAIN)); // internal space
        assertFalse(Email.isValidEmail("e1234567\t" + DOMAIN)); // tab before '@' is not surrounding whitespace
        assertFalse(Email.isValidEmail("e1234567!" + DOMAIN));
        assertFalse(Email.isValidEmail("\u00E91234567" + DOMAIN)); // non-ASCII letter (e with acute accent)
        assertFalse(Email.isValidEmail("\u212Aevin" + DOMAIN)); // KELVIN SIGN lowercases to an ASCII 'k'

        // local part must start and end with a letter or digit
        assertFalse(Email.isValidEmail(".alex" + DOMAIN));
        assertFalse(Email.isValidEmail("alex." + DOMAIN));
        assertFalse(Email.isValidEmail("_alex" + DOMAIN));
        assertFalse(Email.isValidEmail("alex_" + DOMAIN));
        assertFalse(Email.isValidEmail("+alex" + DOMAIN));
        assertFalse(Email.isValidEmail("alex+" + DOMAIN));
        assertFalse(Email.isValidEmail("-alex" + DOMAIN));
        assertFalse(Email.isValidEmail("alex-" + DOMAIN));

        // consecutive dots
        assertFalse(Email.isValidEmail("alex..tan" + DOMAIN));

        // local part length boundaries
        assertTrue(Email.isValidEmail("a" + DOMAIN)); // 1 character
        assertTrue(Email.isValidEmail("a".repeat(64) + DOMAIN)); // 64 characters
        assertFalse(Email.isValidEmail("a".repeat(65) + DOMAIN)); // 65 characters

        // only surrounding spaces and tabs are ignored
        assertTrue(Email.isValidEmail("  e1234567@u.nus.edu\t "));
        assertFalse(Email.isValidEmail("\ne1234567@u.nus.edu")); // line feed
        assertFalse(Email.isValidEmail("e1234567@u.nus.edu\n"));
        assertFalse(Email.isValidEmail("e1234567@u.nus.edu\r")); // carriage return
        assertFalse(Email.isValidEmail("\u0000e1234567@u.nus.edu")); // control character
        assertFalse(Email.isValidEmail("e1234567@u.nus.edu\u0001"));
        assertFalse(Email.isValidEmail("\u00A0e1234567@u.nus.edu")); // no-break space is not stripped

        // valid email
        assertTrue(Email.isValidEmail("e1234567@u.nus.edu"));
        assertTrue(Email.isValidEmail("E1234567@U.NUS.EDU")); // case is canonicalised
        assertTrue(Email.isValidEmail("Kevin@u.nus.edu")); // ASCII 'K'
        assertTrue(Email.isValidEmail("alex.tan" + DOMAIN)); // dot
        assertTrue(Email.isValidEmail("alex_tan" + DOMAIN)); // underscore
        assertTrue(Email.isValidEmail("alex+one" + DOMAIN)); // plus suffix
        assertTrue(Email.isValidEmail("alex-tan" + DOMAIN)); // hyphen
        assertTrue(Email.isValidEmail("a1.b_c+d-e" + DOMAIN)); // mixture of permitted characters
    }

    @Test
    public void equals() {
        Email email = new Email("e1234567@u.nus.edu");

        // same values -> returns true
        assertTrue(email.equals(new Email("e1234567@u.nus.edu")));

        // same canonical value -> returns true, with equal hash codes
        Email uppercaseEmail = new Email(" E1234567@U.NUS.EDU\t");
        assertTrue(email.equals(uppercaseEmail));
        assertEquals(email.hashCode(), uppercaseEmail.hashCode());

        // same object -> returns true
        assertTrue(email.equals(email));

        // null -> returns false
        assertFalse(email.equals(null));

        // different types -> returns false
        assertFalse(email.equals(5.0f));

        // different values -> returns false
        assertFalse(email.equals(new Email("e7654321@u.nus.edu")));

        // plus suffixes and dots are meaningful -> returns false
        assertFalse(new Email("alex+one" + DOMAIN).equals(new Email("alex+two" + DOMAIN)));
        assertFalse(new Email("alex+one" + DOMAIN).equals(new Email("alex" + DOMAIN)));
        assertFalse(new Email("alex.tan" + DOMAIN).equals(new Email("alextan" + DOMAIN)));
    }
}
