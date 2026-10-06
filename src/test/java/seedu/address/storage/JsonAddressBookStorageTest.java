package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.HOON;
import static seedu.address.testutil.TypicalPersons.IDA;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.model.AddressBook;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

public class JsonAddressBookStorageTest {
    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonAddressBookStorageTest");

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
        List<String> records = new ArrayList<>();
        for (int i = 0; i < jsonEscapedNames.length; i++) {
            records.add("{ \"name\": \"" + jsonEscapedNames[i] + "\", \"phone\": \"94351253\", "
                    + "\"email\": \"student" + i + "@u.nus.edu\", "
                    + "\"address\": \"123, Jurong West Ave 6\", \"tags\": [ ] }");
        }
        Path file = testFolder.resolve(fileName);
        Files.writeString(file, "{ \"persons\": [ " + String.join(", ", records) + " ] }");
        return file;
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
