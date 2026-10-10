package seedu.address.logic;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.commands.Command;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.commands.exceptions.DuplicateStudentException;
import seedu.address.logic.parser.AddressBookParser;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.person.Email;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonOrder;
import seedu.address.storage.Storage;

/**
 * The main LogicManager of the app.
 */
public class LogicManager implements Logic {
    public static final String FILE_OPS_ERROR_FORMAT = "Could not save data. No data was changed. Error: %s";

    public static final String FILE_OPS_PERMISSION_ERROR_FORMAT =
            "Could not save data to file %s due to insufficient permissions. No data was changed.";

    public static final String MESSAGE_READ_ONLY = "Data changes are disabled because stored data could not be loaded. "
            + "Restore a valid data file and restart SoCdex.";

    private final Logger logger = LogsCenter.getLogger(LogicManager.class);

    private final Model model;
    private final Storage storage;
    private final AddressBookParser addressBookParser;

    /**
     * Constructs a {@code LogicManager} with the given {@code Model} and {@code Storage}.
     */
    public LogicManager(Model model, Storage storage) {
        this.model = model;
        this.storage = storage;
        addressBookParser = new AddressBookParser();
    }

    @Override
    public CommandResult execute(String commandText) throws CommandException, ParseException {
        return execute(commandText, result -> { });
    }

    /**
     * {@inheritDoc}
     * A pending deletion is cancelled by every submission except a deletion confirmation, which consumes it.
     * The cancellation is reported on the first line of the result or error. A failed command never leaves a
     * deletion pending.
     */
    @Override
    public CommandResult execute(String commandText, Consumer<CommandResult> presentSearch)
            throws CommandException, ParseException {
        logger.info("----------------[USER COMMAND][" + commandText + "]");

        boolean hadPendingDeletion = model.getPendingDeletion().isPresent();
        Command command;
        try {
            command = addressBookParser.parseCommand(commandText);
        } catch (ParseException e) {
            model.clearPendingDeletion();
            throw hadPendingDeletion ? new ParseException(withCancellationNotice(e.getMessage()), e) : e;
        }

        boolean isCancelling = hadPendingDeletion && !command.isDeletionConfirmation();
        if (isCancelling) {
            model.clearPendingDeletion();
            logger.info("Pending deletion cancelled by another command.");
        }

        boolean isSuccessful = false;
        try {
            CommandResult commandResult = executeParsed(command, presentSearch);
            isSuccessful = true;
            return isCancelling ? commandResult.withNotice(Messages.MESSAGE_PENDING_DELETION_CANCELLED) : commandResult;
        } catch (CommandException e) {
            throw isCancelling ? withCancellationNotice(e) : e;
        } finally {
            if (!isSuccessful) {
                // A fresh preview is required after any failure, including a failed replacement preview.
                model.clearPendingDeletion();
            }
        }
    }

    /** Returns {@code message} preceded by the pending-deletion cancellation notice on its own line. */
    private static String withCancellationNotice(String message) {
        return Messages.MESSAGE_PENDING_DELETION_CANCELLED + "\n" + message;
    }

    /** Returns a copy of {@code e} with the cancellation notice, keeping its type and any revealed profile. */
    private static CommandException withCancellationNotice(CommandException e) {
        String message = withCancellationNotice(e.getMessage());
        if (e instanceof DuplicateStudentException duplicate) {
            DuplicateStudentException noticed = new DuplicateStudentException(message, duplicate.getExisting());
            noticed.initCause(e);
            return noticed;
        }
        return new CommandException(message, e);
    }

    /**
     * Runs a parsed command as one transaction: presents its results before saving, saves only changed data and
     * restores the previous roster, filter and order if any step fails.
     */
    private CommandResult executeParsed(Command command, Consumer<CommandResult> presentSearch)
            throws CommandException {
        if (model.isReadOnly() && !command.isReadOnly()) {
            throw new CommandException(MESSAGE_READ_ONLY);
        }

        AddressBook previousData = new AddressBook(model.getAddressBook());
        Runnable restore = model.createRestorePoint();
        CommandResult commandResult;
        try {
            commandResult = command.execute(model);
        } catch (DuplicateStudentException e) {
            restore.run();
            model.updateFilteredPersonList(Model.PREDICATE_SHOW_ALL_PERSONS, PersonOrder.BY_NAME_THEN_EMAIL);
            try {
                presentSearch.accept(CommandResult.forTarget(e.getMessage(), e.getExisting()));
            } catch (RuntimeException | AssertionError displayFailure) {
                restore.run();
                throw new CommandException(Messages.MESSAGE_PROFILE_DISPLAY_FAILURE, displayFailure);
            }
            throw e;
        } catch (CommandException | RuntimeException e) {
            restore.run();
            throw e;
        }

        if (command.isReadOnly() && !previousData.equals(model.getAddressBook())) {
            restore.run();
            throw new CommandException("A read-only command attempted to change roster data. No data was changed.");
        }

        if (commandResult.isUpdateSelection()) {
            try {
                presentSearch.accept(commandResult);
            } catch (RuntimeException | AssertionError e) {
                restore.run();
                String defaultMessage = commandResult.getSelectionTarget() == null
                        ? Messages.MESSAGE_SEARCH_DISPLAY_FAILURE : Messages.MESSAGE_PROFILE_DISPLAY_FAILURE;
                throw new CommandException(command.getDisplayFailureMessage(defaultMessage), e);
            }
        }

        if (previousData.equals(model.getAddressBook())) {
            return commandResult;
        }

        try {
            storage.saveAddressBook(model.getAddressBook());
        } catch (AccessDeniedException e) {
            restore.run();
            throw new CommandException(command.getSaveFailureMessage(
                    String.format(FILE_OPS_PERMISSION_ERROR_FORMAT, e.getMessage())), e);
        } catch (IOException | RuntimeException ioe) {
            restore.run();
            throw new CommandException(command.getSaveFailureMessage(
                    String.format(FILE_OPS_ERROR_FORMAT, ioe.getMessage())), ioe);
        }

        return commandResult;
    }

    @Override
    public boolean isReadOnly() {
        return model.isReadOnly();
    }

    @Override
    public boolean hasPendingDeletion() {
        return model.getPendingDeletion().isPresent();
    }

    @Override
    public boolean cancelPendingDeletion() {
        boolean isCancelled = model.clearPendingDeletion();
        if (isCancelled) {
            logger.info("Pending deletion cancelled without running a command.");
        }
        return isCancelled;
    }

    @Override
    public boolean cancelPendingDeletionUnlessTarget(Person selected) {
        requireNonNull(selected);
        Optional<Email> target = model.getPendingDeletion();
        if (target.isEmpty() || target.get().equals(selected.getEmail())) {
            return false;
        }
        return cancelPendingDeletion();
    }

    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return model.getFilteredPersonList();
    }

    @Override
    public GuiSettings getGuiSettings() {
        return model.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        model.setGuiSettings(guiSettings);
    }
}
