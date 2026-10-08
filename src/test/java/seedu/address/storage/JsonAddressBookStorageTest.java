package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.HOON;
import static seedu.address.testutil.TypicalPersons.IDA;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.model.AddressBook;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.person.Email;
import seedu.address.model.person.GitHub;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Telegram;
import seedu.address.testutil.PersonBuilder;

public class JsonAddressBookStorageTest {
    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonAddressBookStorageTest");
    private static final String SAMPLE_FALSE = "\"sample\": false";
    private static final String SAMPLE_TRUE = "\"sample\": true";

    @TempDir
    public Path testFolder;

    @Test
    public void readAddressBook_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> readAddressBook(null));
    }

    private java.util.Optional<ReadOnlyAddressBook> readAddressBook(String filePath) throws Exception {
        return new JsonAddressBookStorage(Paths.get(filePath)).readAddressBook(addToTestDataPathIfNotNull(filePath));
    }

    private Path addToTestDataPathIfNotNull(String prefsFileInTestDataFolder) {
        return prefsFileInTestDataFolder != null
                ? TEST_DATA_FOLDER.resolve(prefsFileInTestDataFolder)
                : null;
    }

    @Test
    public void read_missingFile_emptyResult() throws Exception {
        assertFalse(readAddressBook("NonExistentFile.json").isPresent());
    }

    @Test
    public void read_notJsonFormat_exceptionThrown() {
        assertThrows(DataLoadingException.class, () -> readAddressBook("notJsonFormatAddressBook.json"));
    }

    @Test
    public void readAddressBook_invalidPersonAddressBook_throwDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readAddressBook("invalidPersonAddressBook.json"));
    }

    @Test
    public void readAddressBook_invalidAndValidPersonAddressBook_throwDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readAddressBook("invalidAndValidPersonAddressBook.json"));
    }

    @Test
    public void readAddressBook_negativeFixtures_failOnlyForIntendedField() throws Exception {
        Path invalidName = TEST_DATA_FOLDER.resolve("invalidPersonAddressBook.json");
        assertLoadRejectedAndFilePreserved(invalidName, Name.MESSAGE_CONSTRAINTS);
        Path invalidEmail = TEST_DATA_FOLDER.resolve("invalidAndValidPersonAddressBook.json");
        assertLoadRejectedAndFilePreserved(invalidEmail, Email.MESSAGE_CONSTRAINTS);

        // positive controls: correcting only the intended field makes each fixture load
        Path correctedName = copyWithReplacement(invalidName, "Hans/Muster", "Hans Muster");
        assertEquals(1, new JsonAddressBookStorage(correctedName).readAddressBook().get().getPersonList().size());
        Path correctedEmail = copyWithReplacement(invalidEmail, "invalid-email", "other@u.nus.edu");
        assertEquals(2, new JsonAddressBookStorage(correctedEmail).readAddressBook().get().getPersonList().size());
    }

    @Test
    public void readAndSaveAddressBook_contactsAndSampleClassification_roundTrip() throws Exception {
        Path filePath = testFolder.resolve("contacts.json");
        AddressBook original = new AddressBook();
        original.addPerson(new PersonBuilder().withName("Alex Tan").withEmail("e9000001@u.nus.edu")
                .withTelegram("Alex_Tan").withGitHub("AlexTan").withSample(true).withRemark("Fictional remark")
                .withTags("demo").withEnrolments(new Enrolment(new ModuleCode("CS2103T"), new Semester("AY26/27 S1")))
                .build());
        original.addPerson(new PersonBuilder().withName("Mei Lim").withEmail("e9000002@u.nus.edu")
                .withGitHub("clear").build());
        original.addPerson(new PersonBuilder().withName("Wei Ong").withEmail("e9000003@u.nus.edu").build());
        JsonAddressBookStorage storage = new JsonAddressBookStorage(filePath);

        storage.saveAddressBook(original);
        assertEquals(original, new AddressBook(storage.readAddressBook().get()));

        // absent contacts are written as null, and every record has an explicit boolean classification
        JsonNode persons = new ObjectMapper().readTree(Files.readString(filePath)).get("persons");
        assertEquals("Alex_Tan", persons.get(0).get("telegram").textValue());
        assertTrue(persons.get(0).get("sample").booleanValue());
        assertTrue(persons.get(1).get("telegram").isNull());
        assertEquals("clear", persons.get(1).get("github").textValue());
        for (JsonNode person : persons) {
            assertTrue(person.get("sample").isBoolean());
        }
        assertTrue(persons.get(2).get("github").isNull());
        assertFalse(persons.get(2).get("sample").booleanValue());
    }

    @Test
    public void readAddressBook_sampleWithoutContactProperties_loadsWithoutContacts() throws Exception {
        Path file = writeRoster("noContacts.json", personJson("Alex Tan", "alex@u.nus.edu"),
                personJsonWith("Mei Lim", "mei@u.nus.edu", SAMPLE_TRUE));
        byte[] originalBytes = Files.readAllBytes(file);

        List<Person> persons = new JsonAddressBookStorage(file).readAddressBook().get().getPersonList();
        assertEquals(List.of(false, true), persons.stream().map(Person::isSample).toList());
        for (Person person : persons) {
            assertTrue(person.getTelegram().isEmpty());
            assertTrue(person.getGitHub().isEmpty());
        }
        assertArrayEquals(originalBytes, Files.readAllBytes(file));
    }

    @Test
    public void readAddressBook_savedFormContacts_success() throws Exception {
        // the JSON Unicode escape for the letter 'A' decodes to a handle that is already in its saved form
        String escapedTelegram = "\\" + "u0041lex_tan";
        Path file = writeRoster("contacts.json",
                personJsonWith("Alex Tan", "e9000001@u.nus.edu", "\"telegram\": \"alex_tan\"",
                        "\"github\": \"Alex-Tan\"", SAMPLE_FALSE),
                personJsonWith("Mei Lim", "e9000002@u.nus.edu", "\"telegram\": null", "\"github\": null",
                        SAMPLE_FALSE),
                personJsonWith("Wei Ong", "e9000003@u.nus.edu", "\"telegram\": \"" + escapedTelegram + "\"",
                        SAMPLE_FALSE));

        List<Person> persons = new JsonAddressBookStorage(file).readAddressBook().get().getPersonList();
        assertEquals("alex_tan", persons.get(0).getTelegram().orElseThrow().value);
        assertEquals("Alex-Tan", persons.get(0).getGitHub().orElseThrow().value);
        assertTrue(persons.get(1).getTelegram().isEmpty());
        assertTrue(persons.get(1).getGitHub().isEmpty());
        assertEquals("Alex_tan", persons.get(2).getTelegram().orElseThrow().value);
    }

    @Test
    public void readAddressBook_invalidContacts_throwDataLoadingExceptionAndPreserveFile() throws Exception {
        // tabs are written as JSON escapes so that the files remain valid JSON
        String[][] cases = {
            {"\"telegram\": \"@alex_tan\"", JsonAdaptedPerson.MESSAGE_NON_NORMALISED_CONTACT},
            {"\"telegram\": \" alex_tan\"", JsonAdaptedPerson.MESSAGE_NON_NORMALISED_CONTACT},
            {"\"telegram\": \"alex_tan \"", JsonAdaptedPerson.MESSAGE_NON_NORMALISED_CONTACT},
            {"\"telegram\": \"alex_tan\\t\"", JsonAdaptedPerson.MESSAGE_NON_NORMALISED_CONTACT},
            {"\"github\": \" alex-tan\"", JsonAdaptedPerson.MESSAGE_NON_NORMALISED_CONTACT},
            {"\"telegram\": \"alex\"", Telegram.MESSAGE_CONSTRAINTS},
            {"\"github\": \"alex--tan\"", GitHub.MESSAGE_CONSTRAINTS},
            {"\"telegram\": 123", String.format(JsonAdaptedPerson.CONTACT_TYPE_MESSAGE_FORMAT, "telegram")},
            {"\"github\": true", String.format(JsonAdaptedPerson.CONTACT_TYPE_MESSAGE_FORMAT, "github")},
            {"\"github\": [ ]", String.format(JsonAdaptedPerson.CONTACT_TYPE_MESSAGE_FORMAT, "github")},
        };
        for (String[] invalidCase : cases) {
            Path file = writeRoster("invalidContact.json", personJson("Alex Tan", "alex@u.nus.edu"),
                    personJsonWith("Mei Lim", "mei@u.nus.edu", invalidCase[0], SAMPLE_FALSE));
            assertLoadRejectedAndFilePreserved(file, invalidCase[1]);
        }
    }

    @Test
    public void readAddressBook_missingOrInvalidSample_throwDataLoadingExceptionAndPreserveFile() throws Exception {
        String missing = String.format(JsonAdaptedPerson.MISSING_FIELD_MESSAGE_FORMAT, JsonAdaptedPerson.SAMPLE_FIELD);
        String[][] cases = {
            {null, missing},
            {"\"sample\": null", missing},
            {"\"sample\": \"true\"", JsonAdaptedPerson.MESSAGE_INVALID_SAMPLE},
            {"\"sample\": 1", JsonAdaptedPerson.MESSAGE_INVALID_SAMPLE},
            {"\"sample\": [ ]", JsonAdaptedPerson.MESSAGE_INVALID_SAMPLE},
            {"\"sample\": { }", JsonAdaptedPerson.MESSAGE_INVALID_SAMPLE},
        };
        for (String[] invalidCase : cases) {
            String[] members = invalidCase[0] == null ? new String[0] : new String[] {invalidCase[0]};
            // the other record is valid, so the whole load is rejected because of this classification alone
            Path file = writeRoster("invalidSample.json", personJson("Alex Tan", "alex@u.nus.edu"),
                    personJsonWith("Mei Lim", "mei@u.nus.edu", members));
            assertLoadRejectedAndFilePreserved(file, invalidCase[1]);
        }
    }

    /**
     * Copies {@code source} to the temporary folder, replacing the only occurrence of {@code target}.
     */
    private Path copyWithReplacement(Path source, String target, String replacement) throws IOException {
        String text = Files.readString(source);
        assertEquals(text.length() - target.length(), text.replace(target, "").length());
        Path copy = testFolder.resolve("corrected-" + source.getFileName());
        Files.writeString(copy, text.replace(target, replacement));
        return copy;
    }

    @Test
    public void readAddressBook_legacyNonNusEmails_throwDataLoadingExceptionAndPreserveFile() throws Exception {
        Path legacyFile = testFolder.resolve("legacyAddressBook.json");
        Files.copy(TEST_DATA_FOLDER.resolve("legacyAb3EmailAddressBook.json"), legacyFile);
        assertLoadRejectedAndFilePreserved(legacyFile, Email.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void readAddressBook_canonicalDistinctEmails_success() throws Exception {
        List<String> emails = List.of("alex.tan@u.nus.edu", "alextan@u.nus.edu", "alex.tan+cs2103@u.nus.edu");
        Path file = writeRoster("canonical.json", personJson("Alex Tan", emails.get(0)),
                personJson("Alex Tan", emails.get(1)), personJson("Alex Tan", emails.get(2)));

        ReadOnlyAddressBook readBack = new JsonAddressBookStorage(file).readAddressBook().get();
        assertEquals(emails, readBack.getPersonList().stream().map(person -> person.getEmail().value).toList());
    }

    @Test
    public void readAddressBook_unicodeEscapedCanonicalEmail_success() throws Exception {
        // The JSON Unicode escape for the letter 'a' decodes to a canonical email
        Path file = writeRoster("escaped.json", personJson("Alex Tan", "\\u0061lex@u.nus.edu"));

        ReadOnlyAddressBook readBack = new JsonAddressBookStorage(file).readAddressBook().get();
        assertEquals("alex@u.nus.edu", readBack.getPersonList().get(0).getEmail().value);
    }

    @Test
    public void readAddressBook_nonCanonicalEmail_throwDataLoadingExceptionAndPreserveFile() throws Exception {
        // tabs are written as JSON escapes so that the files remain valid JSON
        List<String> nonCanonicalEmails = List.of("ALEX@u.nus.edu", "alex@U.NUS.EDU", " alex@u.nus.edu",
                "alex@u.nus.edu ", "\\talex@u.nus.edu", "alex@u.nus.edu\\t");
        for (String nonCanonicalEmail : nonCanonicalEmails) {
            Path file = writeRoster("nonCanonical.json", personJson("Alex Tan", nonCanonicalEmail));
            assertLoadRejectedAndFilePreserved(file, JsonAdaptedPerson.MESSAGE_NON_CANONICAL_EMAIL);
        }
    }

    @Test
    public void readAddressBook_validAndNonCanonicalEmails_throwDataLoadingExceptionAndPreserveFile() throws Exception {
        Path file = writeRoster("mixed.json", personJson("Alex Tan", "alex@u.nus.edu"),
                personJson("Alice Lim", "ALICE@u.nus.edu"));
        assertLoadRejectedAndFilePreserved(file, JsonAdaptedPerson.MESSAGE_NON_CANONICAL_EMAIL);
    }

    @Test
    public void readAddressBook_duplicateCanonicalEmails_throwDataLoadingExceptionAndPreserveFile() throws Exception {
        Path file = writeRoster("duplicate.json", personJson("Alex Tan", "alex@u.nus.edu"),
                personJson("Alexander Tan", "alex@u.nus.edu"));
        assertLoadRejectedAndFilePreserved(file, JsonSerializableAddressBook.MESSAGE_DUPLICATE_PERSON);
    }

    /**
     * Writes a data file with the given JSON person records to the temporary folder and returns its path.
     */
    private Path writeRoster(String fileName, String... personRecords) throws IOException {
        Path file = testFolder.resolve(fileName);
        Files.writeString(file, "{ \"persons\": [ " + String.join(", ", personRecords) + " ] }");
        return file;
    }

    /**
     * Returns a non-sample JSON person record without contacts, with valid details and the given email,
     * which must already be JSON-escaped.
     */
    private static String personJson(String name, String jsonEscapedEmail) {
        return personJsonWith(name, jsonEscapedEmail, SAMPLE_FALSE);
    }

    /**
     * Returns a JSON person record with valid details, the given email and the given complete JSON members for
     * contacts and the sample classification. With no members, the record has neither contacts nor a classification.
     */
    private static String personJsonWith(String name, String jsonEscapedEmail, String... members) {
        StringBuilder record = new StringBuilder("{ \"name\": \"" + name + "\", \"email\": \""
                + jsonEscapedEmail + "\", ");
        for (String member : members) {
            record.append(member).append(", ");
        }
        return record.append("\"tags\": [ ] }").toString();
    }

    /**
     * Asserts that reading {@code file} fails with a stored-data violation with {@code expectedCauseMessage},
     * and that the bytes of {@code file} are unchanged.
     */
    private static void assertLoadRejectedAndFilePreserved(Path file, String expectedCauseMessage)
            throws IOException {
        byte[] originalBytes = Files.readAllBytes(file);

        DataLoadingException exception = Assertions.assertThrows(DataLoadingException.class, () ->
                new JsonAddressBookStorage(file).readAddressBook());
        assertInstanceOf(IllegalValueException.class, exception.getCause());
        assertEquals(expectedCauseMessage, exception.getCause().getMessage());
        assertArrayEquals(originalBytes, Files.readAllBytes(file));
    }

    @Test
    public void readAndSaveAddressBook_allInOrder_success() throws Exception {
        Path filePath = testFolder.resolve("TempAddressBook.json");
        AddressBook original = getTypicalAddressBook();
        JsonAddressBookStorage jsonAddressBookStorage = new JsonAddressBookStorage(filePath);

        // Save in new file and read back
        jsonAddressBookStorage.saveAddressBook(original, filePath);
        ReadOnlyAddressBook readBack = jsonAddressBookStorage.readAddressBook(filePath).get();
        assertEquals(original, new AddressBook(readBack));

        // Modify data, overwrite existing file, and read back
        original.addPerson(HOON);
        original.removePerson(ALICE);
        jsonAddressBookStorage.saveAddressBook(original, filePath);
        readBack = jsonAddressBookStorage.readAddressBook(filePath).get();
        assertEquals(original, new AddressBook(readBack));

        // Save and read without specifying file path
        original.addPerson(IDA);
        jsonAddressBookStorage.saveAddressBook(original); // file path not specified
        readBack = jsonAddressBookStorage.readAddressBook().get(); // file path not specified
        assertEquals(original, new AddressBook(readBack));

    }

    @Test
    public void readAndSaveAddressBook_unicodeNames_success() throws Exception {
        Path filePath = testFolder.resolve("UnicodeNames.json");
        AddressBook original = new AddressBook();
        original.addPerson(new PersonBuilder().withName("\u738B\u5C0F\u660E").withEmail("wang@u.nus.edu").build());
        // supplementary character
        original.addPerson(new PersonBuilder().withName("\uD840\uDC00 Tan").withEmail("tan@u.nus.edu").build());
        // command-style input, normalised by Name before it is saved
        original.addPerson(new PersonBuilder().withName(" Mei \t Lim ").withEmail("mei@u.nus.edu").build());
        // mixed case is preserved
        original.addPerson(new PersonBuilder().withName("aLEX McDonald").withEmail("alex@u.nus.edu").build());
        JsonAddressBookStorage jsonAddressBookStorage = new JsonAddressBookStorage(filePath);

        jsonAddressBookStorage.saveAddressBook(original, filePath);
        ReadOnlyAddressBook readBack = jsonAddressBookStorage.readAddressBook(filePath).get();
        assertEquals(original, new AddressBook(readBack));
        assertEquals("Mei Lim", readBack.getPersonList().get(2).getName().fullName);
        assertEquals("aLEX McDonald", readBack.getPersonList().get(3).getName().fullName);
    }

    @Test
    public void readAddressBook_malformedUnicodeName_throwDataLoadingExceptionAndPreserveFile() throws Exception {
        // the same record with a valid name loads, so the rejection below is caused by the name alone
        Path validFile = writeRosterWithNames("validName.json", "Alex Tan");
        ReadOnlyAddressBook validRoster = new JsonAddressBookStorage(validFile).readAddressBook().get();
        assertEquals(new Name("Alex Tan"), validRoster.getPersonList().get(0).getName());

        // the JSON escape decodes to an isolated high surrogate
        Path malformedFile = writeRosterWithNames("malformedName.json", "Alex\\uD800Tan");
        assertLoadRejectedAndFilePreserved(malformedFile, Name.MESSAGE_CONSTRAINTS);
    }

    @Test
    public void readAddressBook_normalisedNames_success() throws Exception {
        String[] storedNames = {"Alex Tan", "ALEX TAN", "Jos\u00E9 Tan", "\uD840\uDC00 Tan", "Alex\u00A0Tan"};
        Path file = writeRosterWithNames("normalisedNames.json", storedNames);

        List<Person> persons = new JsonAddressBookStorage(file).readAddressBook().get().getPersonList();
        assertEquals(List.of(storedNames), persons.stream().map(person -> person.getName().fullName).toList());
    }

    @Test
    public void readAddressBook_equivalentJsonEscapes_loadSameName() throws Exception {
        // the Java escape writes a literal e-acute; the doubled backslashes write JSON escape text
        Path literalFile = writeRosterWithNames("literalName.json", "Jos\u00E9 Tan");
        Path escapedFile = writeRosterWithNames("escapedName.json", "Jos\\u00E9 Tan");
        Path escapedAsciiFile = writeRosterWithNames("escapedAsciiName.json", "\\u0041lex\\u0020Tan");

        assertEquals(new Name("Jos\u00E9 Tan"), readSoleName(literalFile));
        assertEquals(readSoleName(literalFile), readSoleName(escapedFile));
        assertEquals(new Name("Alex Tan"), readSoleName(escapedAsciiFile));
    }

    @Test
    public void readAddressBook_nonNormalisedName_throwDataLoadingExceptionAndPreserveFile() throws Exception {
        // tabs and escaped spaces are written as JSON escape text, so each file remains valid JSON
        String[] storedNames = {" Alex Tan", "Alex Tan ", "\\tAlex Tan", "Alex Tan\\t",
            "Alex" + " ".repeat(2) + "Tan", "Alex\\tTan", "Alex\\u0020\\u0020Tan"};
        for (String storedName : storedNames) {
            Path file = writeRosterWithNames("nonNormalisedName.json", storedName);
            assertLoadRejectedAndFilePreserved(file, JsonAdaptedPerson.MESSAGE_NON_NORMALISED_NAME);
        }
    }

    @Test
    public void readAddressBook_mixedRosterWithNonNormalisedName_throwDataLoadingException() throws Exception {
        Path file = writeRosterWithNames("mixedNames.json", "Alex Tan", " Mei Lim ");
        assertLoadRejectedAndFilePreserved(file, JsonAdaptedPerson.MESSAGE_NON_NORMALISED_NAME);
    }

    private static Name readSoleName(Path file) throws DataLoadingException {
        return new JsonAddressBookStorage(file).readAddressBook().get().getPersonList().get(0).getName();
    }

    /**
     * Writes a data file with one otherwise valid person for each JSON-escaped name, using distinct emails.
     */
    private Path writeRosterWithNames(String fileName, String... jsonEscapedNames) throws IOException {
        String[] records = new String[jsonEscapedNames.length];
        for (int i = 0; i < jsonEscapedNames.length; i++) {
            records[i] = personJson(jsonEscapedNames[i], "student" + i + "@u.nus.edu");
        }
        return writeRoster(fileName, records);
    }

    @Test
    public void saveAddressBook_nullAddressBook_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveAddressBook(null, "SomeFile.json"));
    }

    /**
     * Saves {@code addressBook} at the specified {@code filePath}.
     */
    private void saveAddressBook(ReadOnlyAddressBook addressBook, String filePath) {
        try {
            new JsonAddressBookStorage(Paths.get(filePath))
                    .saveAddressBook(addressBook, addToTestDataPathIfNotNull(filePath));
        } catch (IOException ioe) {
            throw new AssertionError("There should not be an error writing to the file.", ioe);
        }
    }

    @Test
    public void saveAddressBook_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveAddressBook(new AddressBook(), null));
    }
}
