package seedu.address.logic;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
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

    @Override
    public CommandResult execute(String commandText, Consumer<CommandResult> presentSearch)
            throws CommandException, ParseException {
        logger.info("----------------[USER COMMAND][" + commandText + "]");

        Command command = addressBookParser.parseCommand(commandText);
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
                throw new CommandException(commandResult.getSelectionTarget() == null
                        ? Messages.MESSAGE_SEARCH_DISPLAY_FAILURE : Messages.MESSAGE_PROFILE_DISPLAY_FAILURE, e);
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
