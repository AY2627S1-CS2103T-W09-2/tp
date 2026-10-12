package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.commons.util.AppPaths;
import seedu.address.commons.util.JsonUtil;
import seedu.address.model.ReadOnlyAddressBook;

/**
 * A class to access AddressBook data stored as a JSON file on the hard disk.
 */
public class JsonAddressBookStorage {

    private static final Logger logger = LogsCenter.getLogger(JsonAddressBookStorage.class);

    private Path filePath;
    private final Path homeDirectory;

    public JsonAddressBookStorage(Path filePath) {
        this(filePath, null);
    }

    /** Confines production storage to home; the one-argument constructor supports isolated test fixtures. */
    public JsonAddressBookStorage(Path filePath, Path homeDirectory) {
        this.filePath = filePath;
        this.homeDirectory = homeDirectory;
    }

    private void checkPath(Path path) throws IOException {
        if (homeDirectory != null) {
            AppPaths.requireInside(homeDirectory, path);
        }
    }

    public Path getAddressBookFilePath() {
        return filePath;
    }

    /**
     * Returns AddressBook data as a {@link ReadOnlyAddressBook}.
     * Returns {@code Optional.empty()} if storage file is not found.
     *
     * @throws DataLoadingException if loading the data from storage failed.
     */
    public Optional<ReadOnlyAddressBook> readAddressBook() throws DataLoadingException {
        return readAddressBook(filePath);
    }

    /**
     * Similar to {@link #readAddressBook()}.
     *
     * @param filePath location of the data. Cannot be null.
     * @throws DataLoadingException if loading the data from storage failed.
     */
    public Optional<ReadOnlyAddressBook> readAddressBook(Path filePath) throws DataLoadingException {
        requireNonNull(filePath);
        try {
            checkPath(filePath);
        } catch (IOException e) {
            throw new DataLoadingException(e);
        }

        Optional<JsonSerializableAddressBook> jsonAddressBook = JsonUtil.readJsonFile(
                filePath, JsonSerializableAddressBook.class);
        if (!jsonAddressBook.isPresent()) {
            return Optional.empty();
        }

        try {
            return Optional.of(jsonAddressBook.get().toModelType());
        } catch (IllegalValueException | IllegalArgumentException | NullPointerException ive) {
            logger.info("Illegal values found in " + filePath + ": " + ive.getMessage());
            throw new DataLoadingException(ive);
        }
    }

    /**
     * Saves the given {@link ReadOnlyAddressBook} to the storage.
     * @param addressBook cannot be null.
     * @throws IOException if there was any problem writing to the file.
     */
    public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
        saveAddressBook(addressBook, filePath);
    }

    /**
     * Similar to {@link #saveAddressBook(ReadOnlyAddressBook)}.
     *
     * @param filePath location of the data. Cannot be null.
     */
    public void saveAddressBook(ReadOnlyAddressBook addressBook, Path filePath) throws IOException {
        requireNonNull(addressBook);
        requireNonNull(filePath);

        checkPath(filePath);
        Path target = filePath.toAbsolutePath();
        if (Files.isSymbolicLink(target) || Files.exists(target) && !Files.isWritable(target)) {
            throw new AccessDeniedException(target.toString());
        }
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".socdex-", ".tmp");
        try {
            writeRoster(addressBook, temporary);
            replaceFile(temporary, target);
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException e) {
                // A cleanup failure must not turn a committed save into a reported failure.
                logger.warning("Could not remove temporary roster file " + temporary);
            }
        }
    }

    /** Writes the complete candidate roster without touching the destination. */
    protected void writeRoster(ReadOnlyAddressBook addressBook, Path temporary) throws IOException {
        JsonUtil.saveJsonFile(new JsonSerializableAddressBook(addressBook), temporary);
    }

    /** Atomically replaces the destination, failing if the filesystem cannot provide this operation. */
    protected void replaceFile(Path temporary, Path target) throws IOException {
        Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

}
