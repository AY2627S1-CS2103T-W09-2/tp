package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.storage.JsonAdaptedPerson.MESSAGE_NON_NORMALISED_NAME;
import static seedu.address.storage.JsonAdaptedPerson.MISSING_FIELD_MESSAGE_FORMAT;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.BENSON;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.model.person.Address;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Phone;

public class JsonAdaptedPersonTest {
    private static final String INVALID_NAME = "R/chel";
    private static final String INVALID_PHONE = "+651234";
    private static final String INVALID_ADDRESS = " ";
    private static final String INVALID_EMAIL = "example.com";
    private static final String INVALID_TAG = "#friend";

    private static final String VALID_NAME = BENSON.getName().toString();
    private static final String VALID_PHONE = BENSON.getPhone().toString();
    private static final String VALID_EMAIL = BENSON.getEmail().toString();
    private static final String VALID_ADDRESS = BENSON.getAddress().toString();
    private static final List<JsonAdaptedTag> VALID_TAGS = BENSON.getTags().stream()
            .map(JsonAdaptedTag::new)
            .collect(Collectors.toList());

    @Test
    public void toModelType_validPersonDetails_returnsPerson() throws Exception {
        JsonAdaptedPerson person = new JsonAdaptedPerson(BENSON);
        assertEquals(BENSON, person.toModelType());
    }

    @Test
    public void toModelType_invalidName_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(INVALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = Name.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_nullName_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(null, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, Name.class.getSimpleName());
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_unicodeName_returnsPersonWithName() throws Exception {
        String unicodeName = "\u738B\u5C0F\u660E";
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(unicodeName, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        assertEquals(new Name(unicodeName), person.toModelType().getName());
    }

    @Test
    public void toModelType_overlongOrMalformedName_throwsIllegalValueException() {
        // 101 code points, and an isolated high surrogate inside an otherwise valid name
        for (String invalidName : new String[] {"a".repeat(101), "Alex\uD840Tan"}) {
            JsonAdaptedPerson person =
                    new JsonAdaptedPerson(invalidName, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
            assertThrows(IllegalValueException.class, Name.MESSAGE_CONSTRAINTS, person::toModelType);
        }
    }

    @Test
    public void toModelType_normalisedName_returnsSameStoredName() throws Exception {
        String[] storedNames = {"Alex Tan", "aLEX tAN", "Jos\u00E9 Tan", "\uD840\uDC00 Tan", // supplementary
            "Alex\u00A0Tan", "Alex\u00A0 Tan"}; // a no-break space is significant
        for (String storedName : storedNames) {
            JsonAdaptedPerson person =
                    new JsonAdaptedPerson(storedName, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
            assertEquals(storedName, person.toModelType().getName().fullName);
        }
    }

    @Test
    public void toModelType_nonNormalisedName_throwsIllegalValueException() {
        String[] storedNames = {" Alex Tan", "Alex Tan ", "\tAlex Tan", "Alex Tan\t", // surrounding
            "Alex" + " ".repeat(2) + "Tan", // repeated spaces
            "Alex\tTan", "Alex \tTan", // tabs within the name
            " Jos\u00E9" + " ".repeat(2) + "Tan "};
        for (String storedName : storedNames) {
            JsonAdaptedPerson person =
                    new JsonAdaptedPerson(storedName, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
            assertThrows(IllegalValueException.class, MESSAGE_NON_NORMALISED_NAME, person::toModelType);
        }
    }

    @Test
    public void toModelType_invalidAndNonNormalisedName_reportsNameRuleFirst() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(" Alex/Tan ", VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        assertThrows(IllegalValueException.class, Name.MESSAGE_CONSTRAINTS, person::toModelType);
    }

    @Test
    public void toModelType_invalidPhone_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, INVALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = Phone.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_nullPhone_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, null, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, Phone.class.getSimpleName());
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_invalidEmail_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, INVALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = Email.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_nonCanonicalEmail_throwsIllegalValueException() {
        List<String> nonCanonicalEmails = List.of("JOHND@u.nus.edu", "johnd@U.NUS.EDU",
                " " + VALID_EMAIL, VALID_EMAIL + " ", "\t" + VALID_EMAIL, VALID_EMAIL + "\t");
        for (String nonCanonicalEmail : nonCanonicalEmails) {
            JsonAdaptedPerson person =
                    new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, nonCanonicalEmail, VALID_ADDRESS, VALID_TAGS);
            String expectedMessage = JsonAdaptedPerson.MESSAGE_NON_CANONICAL_EMAIL;
            assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
        }
    }

    @Test
    public void toModelType_nullEmail_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, null, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, Email.class.getSimpleName());
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_invalidAddress_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, INVALID_ADDRESS, VALID_TAGS);
        String expectedMessage = Address.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_nullAddress_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, null, VALID_TAGS);
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, Address.class.getSimpleName());
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_invalidTags_throwsIllegalValueException() {
        List<JsonAdaptedTag> invalidTags = new ArrayList<>(VALID_TAGS);
        invalidTags.add(new JsonAdaptedTag(INVALID_TAG));
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, invalidTags);
        assertThrows(IllegalValueException.class, person::toModelType);
    }

}
