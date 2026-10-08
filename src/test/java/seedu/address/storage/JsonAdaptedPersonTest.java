package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.storage.JsonAdaptedPerson.CONTACT_TYPE_MESSAGE_FORMAT;
import static seedu.address.storage.JsonAdaptedPerson.MESSAGE_NON_NORMALISED_NAME;
import static seedu.address.storage.JsonAdaptedPerson.MISSING_FIELD_MESSAGE_FORMAT;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.BENSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.model.person.Address;
import seedu.address.model.person.Email;
import seedu.address.model.person.GitHub;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Telegram;
import seedu.address.testutil.PersonBuilder;

public class JsonAdaptedPersonTest {
    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

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

    @Test
    public void toModelType_booleanSample_returnsSameClassification() throws Exception {
        for (boolean isSample : new boolean[] {true, false}) {
            JsonAdaptedPerson person = validPerson();
            person.setSample(NODES.booleanNode(isSample));
            assertEquals(isSample, person.toModelType().isSample());
        }
    }

    @Test
    public void toModelType_missingOrNullSample_throwsIllegalValueException() {
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, JsonAdaptedPerson.SAMPLE_FIELD);
        for (JsonNode missing : new JsonNode[] {null, NODES.nullNode()}) {
            JsonAdaptedPerson person = validPerson();
            person.setSample(missing);
            assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
        }
    }

    @Test
    public void toModelType_nonBooleanSample_throwsIllegalValueException() {
        for (JsonNode invalid : new JsonNode[] {NODES.textNode("true"), NODES.textNode("false"), NODES.numberNode(1),
            NODES.numberNode(0), NODES.arrayNode(), NODES.objectNode()}) {
            JsonAdaptedPerson person = validPerson();
            person.setSample(invalid);
            assertThrows(IllegalValueException.class, JsonAdaptedPerson.MESSAGE_INVALID_SAMPLE, person::toModelType);
        }
    }

    @Test
    public void toModelType_omittedOrNullContacts_returnsPersonWithoutContacts() throws Exception {
        for (JsonNode absent : new JsonNode[] {null, NODES.nullNode()}) {
            JsonAdaptedPerson person = validPerson();
            person.setTelegram(absent);
            person.setGitHub(absent);
            Person modelPerson = person.toModelType();
            assertEquals(Optional.empty(), modelPerson.getTelegram());
            assertEquals(Optional.empty(), modelPerson.getGitHub());
        }
    }

    @Test
    public void toModelType_savedFormContacts_returnsSameValues() throws Exception {
        // letter case is preserved, and the literal word clear is an ordinary handle
        for (String telegram : new String[] {"alex_tan", "Alex_Tan", "clear", "a1234", "A" + "b".repeat(31)}) {
            JsonAdaptedPerson person = validPerson();
            person.setTelegram(NODES.textNode(telegram));
            assertEquals(telegram, person.toModelType().getTelegram().orElseThrow().value);
        }
        for (String github : new String[] {"alex-tan", "AlexTan", "clear", "a", "a".repeat(GitHub.MAX_LENGTH)}) {
            JsonAdaptedPerson person = validPerson();
            person.setGitHub(NODES.textNode(github));
            assertEquals(github, person.toModelType().getGitHub().orElseThrow().value);
        }
    }

    @Test
    public void toModelType_nonNormalisedContacts_throwsIllegalValueException() {
        // valid after normalisation, but not already in their saved form
        for (String telegram : new String[] {"@alex_tan", " alex_tan", "alex_tan ", "\talex_tan", "alex_tan\t",
            " @alex_tan "}) {
            JsonAdaptedPerson person = validPerson();
            person.setTelegram(NODES.textNode(telegram));
            assertThrows(IllegalValueException.class, JsonAdaptedPerson.MESSAGE_NON_NORMALISED_CONTACT,
                    person::toModelType);
        }
        for (String github : new String[] {" alex-tan", "alex-tan ", "\talex-tan", "alex-tan\t"}) {
            JsonAdaptedPerson person = validPerson();
            person.setGitHub(NODES.textNode(github));
            assertThrows(IllegalValueException.class, JsonAdaptedPerson.MESSAGE_NON_NORMALISED_CONTACT,
                    person::toModelType);
        }
    }

    @Test
    public void toModelType_invalidContacts_throwsIllegalValueException() {
        for (String telegram : new String[] {"", "alex", "1alex_tan", "alex-tan", "alex tan", "@@alex_tan",
            "A" + "b".repeat(32)}) {
            JsonAdaptedPerson person = validPerson();
            person.setTelegram(NODES.textNode(telegram));
            assertThrows(IllegalValueException.class, Telegram.MESSAGE_CONSTRAINTS, person::toModelType);
        }
        for (String github : new String[] {"", "@alextan", "-alex", "alex-", "alex--tan", "alex_tan",
            "a".repeat(GitHub.MAX_LENGTH + 1)}) {
            JsonAdaptedPerson person = validPerson();
            person.setGitHub(NODES.textNode(github));
            assertThrows(IllegalValueException.class, GitHub.MESSAGE_CONSTRAINTS, person::toModelType);
        }
    }

    @Test
    public void toModelType_nonStringContacts_throwsIllegalValueException() {
        for (JsonNode invalid : new JsonNode[] {NODES.numberNode(123), NODES.booleanNode(true), NODES.arrayNode(),
            NODES.objectNode()}) {
            JsonAdaptedPerson telegramPerson = validPerson();
            telegramPerson.setTelegram(invalid);
            assertThrows(IllegalValueException.class, String.format(CONTACT_TYPE_MESSAGE_FORMAT, "telegram"),
                    telegramPerson::toModelType);
            JsonAdaptedPerson githubPerson = validPerson();
            githubPerson.setGitHub(invalid);
            assertThrows(IllegalValueException.class, String.format(CONTACT_TYPE_MESSAGE_FORMAT, "github"),
                    githubPerson::toModelType);
        }
    }

    @Test
    public void toModelType_invalidContactAndMissingSample_reportsContactFirst() {
        JsonAdaptedPerson person = validPerson();
        person.setTelegram(NODES.textNode("@alex_tan"));
        person.setSample(null);
        assertThrows(IllegalValueException.class, JsonAdaptedPerson.MESSAGE_NON_NORMALISED_CONTACT,
                person::toModelType);
    }

    @Test
    public void constructor_fromPerson_writesContactsAndSampleClassification() throws Exception {
        Person sample = new PersonBuilder(BENSON).withSample(true).build();
        assertEquals(sample, new JsonAdaptedPerson(sample).toModelType());
        Person withoutContacts = new PersonBuilder(BENSON).withoutContacts().build();
        assertEquals(withoutContacts, new JsonAdaptedPerson(withoutContacts).toModelType());
    }

    /**
     * Returns an adapted non-sample person with valid details and no contacts.
     */
    private static JsonAdaptedPerson validPerson() {
        return new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
    }

}
